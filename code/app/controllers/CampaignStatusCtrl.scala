package controllers

import be.objectify.deadbolt.scala.{DeadboltActions, allOfGroup}
import dataaccess.{CampaignDAO, ImagesDAO, KmGroupDAO, KnessetMemberDAO, UserCampaignDAO, UserDAO}
import javax.inject.Inject
import models.{CampaignStatus, UserRole}
import play.api.{Configuration, Logger}
import play.api.i18n.{Langs, Messages, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController}
import security.HearUsSubject

import scala.concurrent.Future

class CampaignStatusCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                   users:UserDAO, campaigns:CampaignDAO, images: ImagesDAO, groups: KmGroupDAO,
                                   userCampaigns:UserCampaignDAO,
                                   langs:Langs, messagesApi:MessagesApi, conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit private val logger = Logger(classOf[CampaignAdminCtrl])
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  def updateStatus = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(cc.parsers.tolerantJson) { implicit req =>
    val json = req.body.as[JsObject]
    campaigns.updateStatus(json("id").asOpt[Long].getOrElse(-1L), CampaignStatus(json("status").asOpt[Int].getOrElse(0))).map(ans => Ok("updated"))
  }

  def changeRequestStatus(id:Long, status:Int) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    val userId = req.subject.get.asInstanceOf[HearUsSubject].user.id
    campaigns.isAllowToEdit(userId, id).flatMap(ans => {
      if (ans) {
        campaigns.updateStatus(id, CampaignStatus(status)).map(c =>
          Ok("updated"))
      }
      else {
        Future(Unauthorized("A user (" + userId + ") cannot edit campaign (" + id + ") without permission."))
      }
    })
  }
}
