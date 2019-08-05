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

class CampaignAdminCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                  users:UsersDAO, campaigns:CampaignDAO, images: ImagesDAO, groups: KmGroupDAO,
                                  userCampaigns:UserCampaignDAO,
                                  langs:Langs, messagesApi:MessagesApi, conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit private val logger = Logger(classOf[CampaignAdminCtrl])
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }



  def showCampaigns = deadbolt.SubjectPresent()() { implicit req =>
    for {
      camps <- campaigns.getAllCampaigns
    } yield Ok(views.html.campaignAdmin.allCampaigns(camps))
  }

  def getCampaigners(searchStr:String) = deadbolt.SubjectPresent()() { implicit req =>
    val sqlSearch = "%"+searchStr.trim+"%"
    users.allCampaigners(Some(sqlSearch)).map( ans => Ok(Json.toJson(ans.map(_.dn))))
  }

  def updateStatus = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(cc.parsers.tolerantJson) { implicit req =>
    val json = req.body.as[JsObject]
    campaigns.updateStatus(json("id").asOpt[Long].getOrElse(-1L), CampaignStatus(json("status").asOpt[Int].getOrElse(0))).map(ans => Ok("updated"))
  }
  
  def deleteCampaign(id:Long, from:String) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    for {
      deleted <- campaigns.deleteCampaign(id)
      camps <- campaigns.getAllCampaigns
    } yield {
      val goTo = from match {
        case "admin" => views.html.campaignAdmin.allCampaigns(camps)
        case "campaigner" => views.html.campaignMgmt.index(camps)
      }
      Ok(goTo).flashing(FlashKeys.MESSAGE -> messagesProvider.messages("campaigns.deleted"))
    }
  }

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
