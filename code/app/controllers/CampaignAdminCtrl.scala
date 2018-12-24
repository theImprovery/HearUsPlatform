package controllers

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions, allOfGroup}
import dataaccess._
import javax.inject.Inject

import models._
import play.api.{Configuration, Logger}
import play.api.i18n.{Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.json.{JsError, JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, ControllerComponents, InjectedController, Result}
import dataaccess.JSONFormats._
import play.api.data.Form
import play.api.data.Forms.number
import security.HearUsSubject
import play.api.data.Forms._
import sun.net.www.protocol.http.AuthenticationHeader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class AdminCampaign(name:String, campaigner:Long)

class CampaignAdminCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                  users:UsersDAO, campaigns:CampaignDAO, images: ImagesDAO, groups: KmGroupDAO,
                                  langs:Langs, messagesApi:MessagesApi, conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  val newCampaignForm = Form(mapping(
    "name"-> text,
    "campaigner" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int])
  )(AdminCampaign.apply)(AdminCampaign.unapply))

  def showCampaigns = deadbolt.SubjectPresent()() { implicit req =>
    for {
      camps <- campaigns.getAllCampaigns
    } yield Ok(views.html.CampaignAdmin.allCampaigns(camps))
  }

  def createCampaign = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    Future(Ok(views.html.CampaignAdmin.createCampaign(newCampaignForm)))
  }

  def getCampaigners(searchStr:String) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    val sqlSearch = "%"+searchStr.trim+"%"
    users.allCampaigners(Some(sqlSearch)).map( ans => Ok(Json.toJson(ans)))
  }

  def updatePublish = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(cc.parsers.tolerantJson) { implicit req =>
    val json = req.body.as[JsObject]
    campaigns.updatePublish(json("id").asOpt[Long].getOrElse(-1L), json("isPublish").asOpt[Boolean].getOrElse(false)).map(ans => Ok("updated"))
  }

  def saveCampaign = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    newCampaignForm.bindFromRequest().fold(
      fwe => {
        Logger.info("errors " + fwe.errors.map(e => fwe.errors(e.key).mkString(", ")).mkString("\n"))
        Future(BadRequest(views.html.CampaignAdmin.createCampaign(fwe)))
      },
      adminCampaign => {
        for {
          usernameExists <- campaigns.campaignNameExists(adminCampaign.name)
          camOpt:Option[Campaign] <- if(!usernameExists) campaigns.add(Campaign(-1l, adminCampaign.name, "", "", "", "", true)).map(Some(_)) else Future(None)
          rel:Option[UserCampaign] <- camOpt.map( cam => campaigns.addUserCampaignRel(UserCampaign(adminCampaign.campaigner, cam.id)
          ).map(Some(_)) ).getOrElse(Future(None))
        } yield {
          var form = newCampaignForm.fill(adminCampaign)
          form = form.withError("name", "error.campaignName.exists")
          rel.map(_=> Redirect(routes.CampaignAdminCtrl.showCampaigns()) )
            .getOrElse( BadRequest(views.html.CampaignAdmin.createCampaign(form)) )
        }
      }
    )
  }

//  def editCampaign(id:Long) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
//    for{
//      campaign <- campaigns.getCampaign(id)
//      rel <- campaigns.ge
//    }
//
//    campaigns.getCampaign(id).map({
//      case None => NotFound("campaign with id " + id + " does not exist")
//      case Some(campaign) => Ok(views.html.CampaignAdmin.createCampaign(newCampaignForm.fill(AdminCampaign(1L, "", 1L) )))
//    })
//  }







  def updateAction = deadbolt.SubjectPresent()()  { implicit req =>
    Future(Ok("gjigjiod"))
  }

  private def campaignEditorAction(camId:Long)(action:Future[Result])(implicit req:AuthenticatedRequest[_]) = {
    val userId = req.subject.get.asInstanceOf[HearUsSubject].user.id
    campaigns.isAllowToEdit(userId, camId).flatMap( ans => {
      if(ans) {
        action
      } else {
        Future(Unauthorized("A user cannot edit campaign without permission."))
      }
    })
  }

}
