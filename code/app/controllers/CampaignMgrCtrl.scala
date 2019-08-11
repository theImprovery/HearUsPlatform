package controllers

import java.sql.{Date, Timestamp}
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions, allOfGroup}
import com.sun.org.glassfish.gmbal.ManagedObjectManager.RegistrationDebugLevel
import dataaccess._
import javax.inject.Inject
import models._
import play.api.{Configuration, Logger}
import play.api.i18n._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController, Result}
import security.HearUsSubject
import dataaccess.JSONFormats._
import org.joda.time.DateTime
import play.api.data.{Form, _}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.mvc.BodyParser.AnyContent

import scala.collection.mutable
import scala.concurrent.Future

case class detailsCampaign(name:String, slug:String, campaigner:Long)

class CampaignMgrCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                campaigns:CampaignDAO, users:UserDAO, usersCampaigns:UserCampaignDAO,
                                groups:KmGroupDAO,
                                langs:Langs, messagesApi:MessagesApi, images: ImagesDAO,
                                conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  private val logger = Logger(classOf[CampaignMgrCtrl])


  val actionForm = Form(
    mapping(
      "id" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "camId" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "kmId" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "actionType" -> nonEmptyText.transform[ActionType.Value]( ActionType.withName, _.toString),
      "date" -> sqlDate.transform[Timestamp]( d => new Timestamp(d.getTime), ts => new Date(ts.getTime)),
      "title" -> nonEmptyText,
      "details" -> text,
      "link" -> text
    )(KmAction.apply)(KmAction.unapply)
  )

  val newCampaignForm = Form(mapping(
    "name" -> text,
    "slug" -> text,
    "campaigner" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int])
  )(detailsCampaign.apply)(detailsCampaign.unapply))

  def index() = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    val userId = req.asInstanceOf[AuthenticatedRequest[_]].subject.get.asInstanceOf[HearUsSubject].user.id
    usersCampaigns.getCampaginsForUser( userId ).map( cmps =>
      Ok( views.html.campaignMgmt.index(cmps.sortBy(_.title)) )
    )
  }

  def createCampaign = deadbolt.SubjectPresent()() { implicit req =>
    val userId = req.subject.asInstanceOf[Option[HearUsSubject]].map(hus => hus.user.id).getOrElse(-1L)
    for {
      campaign <- campaigns.store(Campaign(-1l, "", "", null, "", conf.getOptional[String]("hearUs.defaultCampaignStyle").getOrElse(""),
                "", "", CampaignStatus.WorkInProgress))
      rel <- usersCampaigns.connectUserToCampaign(UserCampaign(userId, campaign.id, isAdmin=true))
      camps <- usersCampaigns.getCampaginsForUser( userId )
    } yield {
      Ok(views.html.campaignMgmt.details(campaign))

    }
  }

  def saveCampaign = deadbolt.SubjectPresent()() { implicit req =>
    newCampaignForm.bindFromRequest().fold(
      fwe => {
        Logger.info("errors " + fwe.errors.map(e => fwe.errors(e.key).mkString(", ")).mkString("\n"))
        Future(BadRequest(views.html.campaignMgmt.createCampaign(fwe)))
      },
      adminCampaign => {
        for {
          slugExists <- campaigns.campaignSlugExists(adminCampaign.slug)
          camOpt:Option[Campaign] <- {
            if (!slugExists) campaigns.store(Campaign(-1l, adminCampaign.name, "", Some(adminCampaign.slug), "",
              conf.getOptional[String]("hearUs.defaultCampaignStyle").getOrElse(""), "", "", CampaignStatus.WorkInProgress)).map(Some(_))
            else Future(None)
          }
          rel:Option[UserCampaign] <- camOpt.map( cam => usersCampaigns.connectUserToCampaign(UserCampaign(adminCampaign.campaigner, cam.id, isAdmin=true)
          ).map(Some(_)) ).getOrElse(Future(None))
        } yield {
          var form = newCampaignForm.fill(adminCampaign)
          form = form.withError("slug", "error.campaignSlug.exists")
          rel.map(_=> Redirect(routes.CampaignAdminCtrl.showCampaigns()) )
            .getOrElse( BadRequest(views.html.campaignMgmt.createCampaign(form)) )
        }
      }
    )
  }

  def editCampaign(camId:Long) = deadbolt.SubjectPresent()() { implicit req =>
    campaignEditorAction(camId){
      for {
        campaign <- campaigns.getCampaign(camId)
        imageOp <- images.getImageForCampaign(camId)
        knessetMembers <- kms.getAllKms
        positions <- campaigns.getPositions(camId)
        actions <- campaigns.getActions(camId)
        parties <- kms.getAllParties
      } yield {
        campaign.map( c => Ok(views.html.knesset.campaignEditor(c, Position.values.toSeq, Platform.values.toSeq, imageOp,
          conf.get[String]("hearUs.files.camImages.url"), knessetMembers.sortBy(_.name),
          positions.map(p => (p.kmId, p.position.toString)).toMap, actions,
          parties.map(p => (p.id, p.name)).toMap)))
          .getOrElse(NotFound("campaign with id " + camId + "does not exist"))
      }
    }

  }


  def details(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(id){
      for {
        campaignOpt <- campaigns.getCampaign(id)
      } yield campaignOpt.map(c => Ok(views.html.campaignMgmt.details(c))).getOrElse(NotFound("campaign with id " + id + "does not exist"))
    }
  }

  def updateDetails(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(cc.parsers.tolerantJson) { implicit req =>
    campaignEditorAction(id){
      req.body.validate[CampaignDetails].fold(
        errors => Future(BadRequest(Json.obj("message" -> "can't parse campaign", "details"->errors.mkString(",")))),
        campaignDtls => {
          campaigns.updateDetails(id,campaignDtls).map(newC => Ok(Json.toJson(newC)))
        }
      )
    }
  }
  
  def showFrontPageEditor( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id){
      for {
        campaignOpt <- campaigns.getCampaign(id)
        text    <- campaigns.getTextsFor(id).map( _.getOrElse(CampaignText(id, "", "", "", "", "", "")))
      } yield {
        campaignOpt match {
          case None => NotFound( views.html.errorPage(404, Messages("errors.campaignNotFound")) )
          case Some(campaign) => Ok(views.html.campaignMgmt.frontPageEditor(campaign, text, Position.values, Gender.values))
        }
      }
    
    }
  }
  
  val frontPageForm = Form(
    mapping(
      "campaignId" -> ignored(0l), // we get that from the url.
      "title"->text,
      "subtitle" -> text,
      "bodyText" -> text,
      "footer" -> text,
      "groupLabels" -> text,
      "kmLabels" -> text
  )(CampaignText.apply)(CampaignText.unapply))

  def apiUpdateFrontPage( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(cc.parsers.tolerantJson) { implicit req =>
    campaignEditorAction(id) {
      req.body.validate[CampaignText].fold(
        errors => {
          logger.info(errors.mkString(","))
          Future(BadRequest(Json.obj("message" -> "can't parse campaign", "details"->errors.mkString(","))))
        },
        texts => {
          campaigns.updateTexts(texts.copy(campaignId=id)).map( i =>
              Ok( Json.toJson(i)))
        }
      )
    }
  }

  def updateFrontPage( id:Long )  = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(id) {
      frontPageForm.bindFromRequest().fold(
        fwe => {
          logger.info("err" + fwe.errors.mkString("\n"))
          Future(BadRequest(fwe.errors.mkString("\n")))
        },
        texts => {
          campaigns.updateTexts(texts.copy(campaignId=id)).map( i =>
            Redirect( routes.CampaignMgrCtrl.showFrontPageEditor(id))
              .flashing(FlashKeys.MESSAGE ->
                          Informational(InformationalLevel.Success, Messages("campaignMgmt.frontPage.saved")).encoded)
          )

        }
      )
    }
  }
  
  def editMessages(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(id) {
      for {
        campaignOpt <- campaigns.getCampaign(id)
      } yield campaignOpt match {
        case None => NotFound("campaign with id " + id + "does not exist")
        case Some(c) => Ok(views.html.campaignMgmt.messages(c, Position.values.toSeq))
      }
    }
  }
  
  def positions(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(id) {
      for {
        campaignOpt <- campaigns.getCampaign(id)
        knessetMembers <- kms.getAllKms
        positions <- campaigns.getPositions(id)
        parties <- kms.getAllParties
      } yield campaignOpt.map(c => Ok(views.html.campaignMgmt.positions(c, knessetMembers.sortBy(_.name),
                                        Position.values.toSeq, positions.map(p => (p.kmId, p.position.toString)).toMap,
                                        parties.map(p => (p.id, p.name)).toMap))).getOrElse(NotFound("campaign with id " + id + "does not exist"))
    }
  }

  def allActions(camId:Long, kmId:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(camId) {
      for {
        campaignOpt <- campaigns.getCampaign(camId)
        knessetMember <- campaignOpt.map(_ => kms.getKM(kmId)).getOrElse(Future(None))
        actions <- campaigns.getActions(camId, kmId)
        party <- knessetMember.map( km=> kms.getParty(km.partyId) ).getOrElse(Future(None))
      } yield knessetMember.map(km => Ok(views.html.campaignMgmt.actions(campaignOpt.get, km, party, actions))).getOrElse(NotFound("campaign with id " + camId + "does not exist"))
    }
  }

  def showNewAction(camId:Long, kmId:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(camId) {
      for {
        campaignOpt <- campaigns.getCampaign(camId)
        knessetMember: Option[KnessetMember] <- campaignOpt.map(_ => kms.getKM(kmId)).getOrElse(Future(None))
      } yield knessetMember.map(km => Ok(views.html.campaignMgmt.kmActionEditor(actionForm, km, campaignOpt.get, ActionType.values.toSeq))).getOrElse(NotFound("campaign with id " + camId + "does not exist"))
    }
  }

  def editAction(id:Long, camId:Long, kmId:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(camId) {
      for {
        actionOpt <- campaigns.getAction(id)
        campaignOpt <- actionOpt.map( _ => campaigns.getCampaign(camId) ).getOrElse(Future(None))
        knessetMember: Option[KnessetMember] <- campaignOpt.map(_ => kms.getKM(kmId)).getOrElse(Future(None))
      } yield knessetMember.map(km => Ok(views.html.campaignMgmt.kmActionEditor(actionForm.fill(actionOpt.get), km, campaignOpt.get, ActionType.values.toSeq))).getOrElse(NotFound("campaign with id " + camId + "does not exist"))
    }
  }

  def saveAction(camId:Long, kmId:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    actionForm.bindFromRequest().fold(
      formWithErrors => {
        for {
          campaignOpt <- campaigns.getCampaign(camId)
          knessetMember: Option[KnessetMember] <- campaignOpt.map(_ => kms.getKM(kmId)).getOrElse(Future(None))
        } yield {
          Logger.info(formWithErrors.errors.mkString("\n"))
          knessetMember.map(km => BadRequest(views.html.campaignMgmt.kmActionEditor(formWithErrors, km, campaignOpt.get, ActionType.values.toSeq)))
            .getOrElse(NotFound("campaign with id " + camId + "does not exist"))
        }
      },
      action => {
        campaignEditorAction(camId){
          val message = Informational(InformationalLevel.Success, Messages("action.update"))
          campaigns.updateAction(action).map(newAction => Redirect(routes.CampaignMgrCtrl.allActions(action.camId, action.kmId)).flashing(FlashKeys.MESSAGE -> message.encoded))
        }
      }
    )

  }

  def deleteAction(id:Long, camId:Long, kmId:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(camId){
      for {
        deleted       <- campaigns.deleteAction(id)
        campaignOpt   <- campaigns.getCampaign(camId)
        knessetMember <- kms.getKM(kmId)
        party <- knessetMember.map( km=> kms.getParty(km.partyId) ).getOrElse(Future(None))
        actions <- campaigns.getActions(camId, kmId)
      } yield knessetMember.map(km => Ok(views.html.campaignMgmt.actions(campaignOpt.get, km, party, actions))).getOrElse(NotFound("campaign with id " + camId + "does not exist"))
    }

  }

  def updatePosition = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[KmPosition].fold(
      errors => {
        Logger.info("errors " + errors.mkString("\n"))
        Future(BadRequest("can't update position"))
      },
      pos => {
        campaignEditorAction(pos.camId){
          campaigns.updatePosition(pos).map(ans => Ok(Json.toJson(ans)))
        }
      }
    )
  }

  def getLabelText(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    campaignEditorAction(id)(campaigns.getLabelText(id).map(lts => Ok(Json.toJson(lts))))
  }

  def updateLabels() = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Seq[LabelText]].fold(
      errors => Future({
        Logger.info("errors " + errors.mkString("\n"))
        BadRequest("can't parse label text")
      }),
      labels => {
        campaignEditorAction(labels.head.camId){
          campaigns.addLabelTexts(labels).map(ans => Ok(Json.toJson(ans)))
        }
      }
    )
  }
  
  def getMessages(id:Long) = Action.async{implicit req =>
    campaigns.getMessages(id).map(ms => Ok(Json.toJson(ms)))
  }

  def updateMessages(id:Long) = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    campaignEditorAction(id){
      req.body.validate[Seq[CannedMessage]].fold(
        errors => {
          Logger.info("errors " + errors.mkString("\n"))
          Future(BadRequest(Json.obj("message"->"can't parse canned message", "details"->errors.mkString("\n"))))
        },
        msgs => {
          campaigns.setMessages(id, msgs).map(_ => Ok(Json.obj("message"->"Messages Saved")))
        }
      )
    }
  }

  def getSocialMedia(id:Long) = deadbolt.SubjectPresent()() {implicit req =>
    campaigns.getSm(id).map( sm => Ok(Json.toJson(sm)))
  }

  def updateSocialMedia() = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Seq[SocialMedia]].fold(
      errors => {
        Logger.info("errors " + errors.mkString("\n"))
        Future(BadRequest("can't parse social media details"))
      },
      sms => {
        campaignEditorAction(sms.head.camId){
          campaigns.addSm(sms).map(ans => Ok(Json.toJson(ans)))
        }
      }
    )
  }

  def showCampaignTeam( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id) {
      for {
        campaign <- campaigns.getCampaign(id)
        team     <- usersCampaigns.getTeam(id)
      } yield campaign match {
        case None => NotFound("Can't find campaign")
        case Some(c) => {
          val curUser = req.subject.get.asInstanceOf[HearUsSubject].user
          Ok( views.html.campaignMgmt.campaignTeam(c, team, curUser )
          )
        }
      }
    }
  }
  
  val userIdForm = Form( single(
    "userId" -> longNumber
  ))
  
  def doAddToTeam(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id) {
      userIdForm.bindFromRequest().fold(
        fwe => {
          Logger.warn( fwe.errors.mkString("\n") )
          Future(BadRequest("Error"))
        },
        userId => {
          for {
            userOpt <- users.get(userId)
            didAdd <- {
              if (userOpt.isDefined)
                usersCampaigns.connectUserToCampaign(UserCampaign(userId, id, isAdmin=false)).map(_ => true)
              else Future(false)
            }
          } yield {
            if ( didAdd ) {
               Redirect(routes.CampaignMgrCtrl.showCampaignTeam(id)).flashing(
                  FlashKeys.MESSAGE->Informational(InformationalLevel.Success,
                                                    Messages("campaignMgmt.team.userAdded", userOpt.get.username)).encoded)
            } else {
              NotFound("Can't add to team. Are you sure the user exist?")
  }}})}}
  
  def doMakeAdminInTeam(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id) {
      userIdForm.bindFromRequest().fold(
        fwe => {
          Logger.warn( fwe.errors.mkString("\n") )
          Future(BadRequest("Error"))
        },
        userId => {
          for {
            userOpt <- users.get(userId)
            madeAdmin <- {
              if (userOpt.isDefined) usersCampaigns.connectUserToCampaign(UserCampaign(userId, id, isAdmin=true)).map(_=>true)
              else Future(false)
            }
          } yield {
            if ( madeAdmin ){
              Redirect(routes.CampaignMgrCtrl.showCampaignTeam(id)).flashing(
                FlashKeys.MESSAGE->Informational(InformationalLevel.Success,
                  Messages("campaignMgmt.team.userMadeAdmin", userOpt.get.username)).encoded)
            } else {
              NotFound("Can't add to team. Are you sure the user exist?")
            }
  }})}}
  
  
  def doRemoveAdminInTeam(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id) {
      userIdForm.bindFromRequest().fold(
        fwe => {
          Logger.warn( fwe.errors.mkString("\n") )
          Future(BadRequest("Error"))
        },
        userId => {
          for {
            userOpt <- users.get(userId)
            removed <- if (userOpt.isDefined) {
              usersCampaigns.removeAdminFromTeam(userId, id)
            } else Future(false)
          } yield {
            if ( userOpt.isDefined ) {
              if ( removed ) {
                Redirect(routes.CampaignMgrCtrl.showCampaignTeam(id)).flashing(
                  FlashKeys.MESSAGE->Informational(InformationalLevel.Success,
                    Messages("campaignMgmt.team.userRemovedFromAdmin", userOpt.get.username)).encoded)
              } else {
                Redirect(routes.CampaignMgrCtrl.showCampaignTeam(id)).flashing(
                  FlashKeys.MESSAGE->Informational(InformationalLevel.Danger,
                    Messages("campaignMgmt.team.cantRemoveLastAdmin"),
                    Messages("campaignMgmt.team.cantRemoveLastAdmin.details")).encoded)
              }
            } else {
              NotFound("User not found")
            }
  }})}}
  
  def doRemoveFromTeam(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id) {
      userIdForm.bindFromRequest().fold(
        fwe => {
          Logger.warn( fwe.errors.mkString("\n") )
          Future(BadRequest("Error"))
        },
        userId => {
          for {
            userOpt <- users.get(userId)
            userPresent = userOpt.isDefined
            removed <- if (userPresent) usersCampaigns.removeFromTeam(userId, id) else Future(false)
          } yield {
            (userPresent, removed) match {
              case (false, _) => NotFound("Can't add to team. Are you sure the user exist?")
              case (true, false) => Redirect(routes.CampaignMgrCtrl.showCampaignTeam(id)).flashing(
                  FlashKeys.MESSAGE->Informational(InformationalLevel.Danger,
                                                    Messages("campaignMgmt.team.cantRemoveLastAdmin"),
                                                    Messages("campaignMgmt.team.cantRemoveLastAdmin.details")).encoded)
              case (true, true) => Redirect(routes.CampaignMgrCtrl.showCampaignTeam(id)).flashing(
                  FlashKeys.MESSAGE->Informational(InformationalLevel.Success, Messages("campaignMgmt.team.userRemovedFromTeam",
                                                                                          userOpt.get.username)).encoded)
  }}})}}

  def showCampaignDesign( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id) {
      for {
        campaign <- campaigns.getCampaign(id)
        campImg  <- images.getImageForCampaign(id)
      } yield campaign match {
        case None => NotFound("Can't find campaign")
        case Some(c) => {
          val csses = c.themeData.split("/*---*/")
          val cssMap = parseCampaignDesign(csses(0))
          Ok( views.html.campaignMgmt.design(c, Position.values.toSeq,
            campImg, conf.get[String]("hearUs.files.campaignImages.url"),
            cssMap, if (csses.length>1){csses(1)}else{""})
          )
        }
      }
    }
  }

  def showCampaignGroups( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(){ implicit req =>
    campaignEditorAction(id) {
      for {
        campaign <- campaigns.getCampaign(id)
          groups <- groups.getGroupsForCampaign(id)
      } yield campaign match{
        case None => NotFound("Can't find campaign")
        case Some(c) => {
          val curUser = req.subject.get.asInstanceOf[HearUsSubject].user
          Ok( views.html.campaignMgmt.campaignGroups(c, groups, curUser )
          )
        }
      }
    }
  }


  def getGroups(searchStr:String) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    val sqlSearch = "%"+searchStr.trim+"%"
    groups.allGroupsDN(Some(sqlSearch)).map( ans => Ok(Json.toJson(ans)))
  }

  val groupIdForm = Form( single(
    "groupId" -> longNumber
  ))

  def doAddGroupToCampaign( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(id) {
      groupIdForm.bindFromRequest().fold(
        fwe => {
          Logger.warn( fwe.errors.mkString("\n") )
          Future(BadRequest("Error"))
        },
        groupId => {
          for {
            groupOpt <- groups.getGroupDN(groupId)
            added <- if (groupOpt.isDefined)
              groups.addGroupToCampaign(RelevantGroup(id, groupId)).map(_ => true)
            else Future(false)
          } yield {
            if (added) {
              Redirect(routes.CampaignMgrCtrl.showCampaignGroups(id)).flashing(
                FlashKeys.MESSAGE -> Informational(InformationalLevel.Success,
                  Messages("campaignMgmt.groups.groupAdded", groupOpt.get.name)).encoded)
            } else {
              NotFound("Can't find group " + groupId)
            }
          }
        }
      )
    }
  }

  def doRemoveGroupFromCampaign( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(cc.parsers.tolerantJson) { implicit req =>
    campaignEditorAction(id) {
      req.body.validate[JsObject].fold(
        fwe => {
          Logger.warn( fwe.mkString("\n") )
          Future(BadRequest("Error"))
        },
        json => {
          val groupId = json("groupId").as[Long]
          for {
            groupOpt <- groups.getGroupDN(groupId)
          } yield groupOpt match {
            case None => NotFound("Can't find group " + groupId)
            case Some(group) => {
              groups.removeGroupFromCamp(id, groupId)
              Ok("").flashing(
                FlashKeys.MESSAGE->Informational(InformationalLevel.Success, Messages("campaignMgmt.groups.groupRemoved")).encoded)
            }
          }
        })
    }
  }
  
  val designForm = Form( tuple(
    "css"->text,
    "imageCredit"->text
  ))
  
  def doUpdateCampaignDesign( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(cc.parsers.multipartFormData) { implicit req =>
    campaignEditorAction(id) {
      val bf = designForm.bindFromRequest()
      
      val fileOpt = req.body.file("imageFile")
      for {
        designUpdated <- campaigns.updateDesign(id, bf.get._1)
        _ = fileOpt.foreach(tf => images.storeCampaignImageFile(id, tf) )
        campaignImage <- images.getImageForCampaign(id)
        newImageOpt   = fileOpt.map(file=>KMImage(campaignImage.map(_.id).getOrElse(0), None, Some(id), file.filename.split("\\.").last,
                                                   file.contentType.getOrElse("application/octet-stream"),
                                                   new Timestamp(System.currentTimeMillis()), bf.get._2))
        updatedImageOpt = campaignImage.map( ci => ci.copy(credit=bf.get._2) )
        updateImageRes <- Seq(newImageOpt, updatedImageOpt).flatten.headOption.map( images.storeImage ).getOrElse(Future(None))
      } yield {
        Ok("done. Design Updated:" + designUpdated + " savedImage:" + updateImageRes)
      }
    }
  }
  
  def deleteCampaignImage(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(id) {
      for {
        image <- images.getImageForCampaign(id)
        deletedCount <- image.map( img => images.deleteImageRecord(img.id) ).getOrElse(Future(0))
      } yield {
        image.filter( _ => deletedCount>0 ).foreach( img => images.deleteCampaignImageFile(img) )
        Ok("Deleted")
      }
    }
  }

  def apiCheckAndUpdateSlug(id:Long) = deadbolt.SubjectPresent()(cc.parsers.text) { implicit req =>
    val slug = req.body
    if("^[A-Za-z0-9_-]+$".r.findFirstIn(slug).nonEmpty) {
      campaigns.updateSlug(id, slug).map(ans => Ok(Json.toJson(ans)))
    }else{
      Future(BadRequest("Slug should match to A-Za-z1-9_-"))
    }
  }
  
  private def campaignEditorAction(camId:Long)(action:Future[Result])(implicit req:AuthenticatedRequest[_]) = {
    val userId = req.subject.get.asInstanceOf[HearUsSubject].user.id
    campaigns.isAllowToEdit(userId, camId).flatMap( ans => {
      if(ans) {
        action
      } else {
        Future(Unauthorized("A user (" + userId +") cannot edit campaign (" + camId + ") without permission."))
      }
    })
  }
  
  private def parseCampaignDesign(css:String):Map[(String,String),String] = {
    val lines = css.split("\n").map(_.trim).filter(_.nonEmpty)
    val retVal = mutable.Map[(String,String),String]()
    var curSelector = ""
    lines.foreach(line => {
      if ( line.endsWith("{") ) {
        curSelector = line.split(" ")(0)
      } else if ( line.contains(":")) {
        val kv = line.replaceAll(";", "").split(":").map(_.trim)
        retVal((curSelector,kv(0))) = kv(1)
      }
    })
    retVal.toMap
  }
}
