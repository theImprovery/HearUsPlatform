package controllers

import be.objectify.deadbolt.scala.DeadboltActions
import dataaccess._
import javax.inject.Inject
import models._
import play.api.{Configuration, Logger}
import play.api.i18n.{Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.json.{JsError, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController}
import dataaccess.JSONFormats._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CampaignCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                             campaigns:CampaignDAO, images: ImagesDAO, groups: KmGroupDAO, langs:Langs, messagesApi:MessagesApi,
                             conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  def showCampaigns = deadbolt.SubjectPresent()() { implicit req =>
    for {
      camps <- campaigns.getAllCampaigns
    } yield Ok(views.html.knesset.campaigns(camps))
  }

  def createCampaign = deadbolt.SubjectPresent()() { implicit req =>
    Logger.info("create")
    val newCamp = Campaign(-1L, "קמפיין חדש", "", "", "", "")
    campaigns.add(newCamp).map(stored => Redirect(routes.CampaignCtrl.editCampaign( stored.id )))
  }

  def editCampaign(camId:Long) = deadbolt.SubjectPresent()() { implicit req =>
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

  def updateDetails() = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Campaign].fold(
      errors => Future(BadRequest("can't parse campaign")),
      campaign => {
        campaigns.add(campaign).map(newC => Ok(Json.toJson(newC)))
      }
    )
  }

  def deleteCampaign(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for {
      deleted <- campaigns.deleteCampaign(id)
      camps <- campaigns.getAllCampaigns
    } yield {
      Ok(views.html.knesset.campaigns(camps)).flashing(FlashKeys.MESSAGE -> messagesProvider.messages("campaigns.deleted"))
    }
  }

  def getLabelText(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    campaigns.getLabelText(id).map(lts => Ok(Json.toJson(lts)))
  }

  def updateLabels() = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Seq[LabelText]].fold(
      errors => Future({
        Logger.info("errors " + errors.mkString("\n"))
        BadRequest("can't parse label text")
      }),
      labels => {
        campaigns.addLabelTexts(labels).map(ans => Ok(Json.toJson(ans)))
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
        campaigns.addMessages(msgs).map(ans => Ok(Json.toJson(ans)))
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
        campaigns.addSm(sms).map(ans => Ok(Json.toJson(ans)))
      }
    )
  }

  def updatePosition = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[KmPosition].fold(
      errors => {
        Logger.info("errors " + errors.mkString("\n"))
        Future(BadRequest("can't update position"))
      },
      pos => campaigns.updatePosition(pos).map(ans => Ok(Json.toJson(ans)))
    )
  }

  def updateAction = deadbolt.SubjectPresent()()  { implicit req =>
    Future(Ok("gjigjiod"))
  }

}
