package controllers

import java.sql.{Date, Timestamp}
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions, allOfGroup}
import dataaccess._
import javax.inject.Inject

import models._
import play.api.{Configuration, Logger}
import play.api.i18n._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController, Result}
import security.HearUsSubject
import dataaccess.JSONFormats._
import org.joda.time.DateTime
import play.api.data.{Form, _}
import play.api.data.Forms._
import play.api.data.format.Formatter

import scala.concurrent.Future

class CampaignMgrCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                campaigns:CampaignDAO, users:UsersDAO, usersCampaigns:UserCampaignDAO,
                                langs:Langs, messagesApi:MessagesApi, images: ImagesDAO,
                                conf:Configuration, ws:WSClient) extends InjectedController {
  
  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }


  val actionForm = Form(
    mapping(
      "id" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "camId" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "kmId" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "actionType" -> nonEmptyText.transform[ActionType.Value]( ActionType.withName(_), _.toString),
      "date" -> sqlDate.transform[Timestamp]( d => new Timestamp(d.getTime), ts => new Date(ts.getTime)),
      "title" -> nonEmptyText,
      "details" -> text,
      "link" -> text
    )(KmAction.apply)(KmAction.unapply)
  )

  def index() = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    val userId = req.asInstanceOf[AuthenticatedRequest[_]].subject.get.asInstanceOf[HearUsSubject].user.id
    usersCampaigns.getCampaginsForUser( userId ).map( cmps =>
      Ok( views.html.campaignMgmt.index(cmps.sortBy(_.title)) )
    )
  }

  def editCampaign(camId:Long) = deadbolt.SubjectPresent()() { implicit req =>
    campaignEditorAction(camId){
      for {
        campaign <- campaigns.getCampaign(camId)
        imageOp <- images.getImageForCamps(camId)
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

  def settings(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    campaignEditorAction(id){
      for {
        campaignOpt <- campaigns.getCampaign(id)
        imageOp <- images.getImageForCamps(id)
      } yield campaignOpt.map(c => Ok(views.html.campaignMgmt.settings(c, Platform.values.toSeq, Position.values.toSeq, imageOp, conf.get[String]("hearUs.files.camImages.url")))).getOrElse(NotFound("campaign with id " + id + "does not exist"))
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
        knessetMember: Option[KnessetMember] <- campaignOpt.map(_ => kms.getKM(kmId)).getOrElse(Future(None))
        actions <- campaigns.getActions(camId, kmId)
      } yield knessetMember.map(km => Ok(views.html.campaignMgmt.actions(campaignOpt.get, km, actions))).getOrElse(NotFound("campaign with id " + camId + "does not exist"))
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
        actions <- campaigns.getActions(camId, kmId)
      } yield knessetMember.map(km => Ok(views.html.campaignMgmt.actions(campaignOpt.get, km, actions))).getOrElse(NotFound("campaign with id " + camId + "does not exist"))
    }

  }

  def updateDetails = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Campaign].fold(
      errors => Future(BadRequest("can't parse campaign")),
      campaign => {
        campaignEditorAction(campaign.id){
          campaigns.add(campaign).map(newC => Ok(Json.toJson(newC)))
        }
      }
    )
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

  def getMessages(id:Long) = deadbolt.SubjectPresent()() {implicit req =>
    campaigns.getMessages(id).map(ms => Ok(Json.toJson(ms)))
  }

  def updateMessages() = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Seq[CannedMessage]].fold(
      errors => {
        Logger.info("errors " + errors.mkString("\n"))
        Future(BadRequest("can't parse canned message"))
      },
      msgs => {
        campaignEditorAction(msgs.head.camId){
          campaigns.addMessages(msgs).map(ans => Ok(Json.toJson(ans)))
        }
      }
    )
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

  def deleteCampaign(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    campaignEditorAction(id){
      for {
        deleted <- campaigns.deleteCampaign(id)
        camps <- campaigns.getAllCampaigns
      } yield {
        Ok(views.html.knesset.campaigns(camps)).flashing(FlashKeys.MESSAGE -> messagesProvider.messages("campaigns.deleted"))
      }
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
}
