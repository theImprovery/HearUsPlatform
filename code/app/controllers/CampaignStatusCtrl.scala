package controllers

import actors.EmailSendingActor
import actors.InvalidateCacheActor.InvalidateCampaignById
import akka.actor.ActorRef
import be.objectify.deadbolt.scala.{DeadboltActions, allOfGroup}
import dataaccess.{CampaignDAO, ImagesDAO, KmGroupDAO, KnessetMemberDAO, UserCampaignDAO, UserDAO}
import javax.inject.{Inject, Named}
import models.{CampaignStatus, UserRole}
import play.api.{Configuration, Logger}
import play.api.i18n.{Langs, Messages, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController}
import security.HearUsSubject

import scala.concurrent.Future

class CampaignStatusCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents,
                                   campaigns: CampaignDAO,
                                   @Named("cacheInvalidator")uncacheActor:ActorRef,
                                   @Named("email-actor")emailActor:ActorRef,
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
    campaigns.isAllowedToManage(req.subject.get.asInstanceOf[HearUsSubject], id).flatMap(ans => {
      if (ans) {
        val newStatus = CampaignStatus(status)
        for {
          update <- campaigns.updateStatus(id, newStatus)
        } yield {
          if ( newStatus == CampaignStatus.PublicationRequested ) {
            emailActor ! EmailSendingActor.PublicationRequest(id)
          }
          Ok("updated")
        }
        
      }
      else {
        Future(Unauthorized("Permission Denied"))
      }
    })
  }
  
  def takePublishedCampaignDown( id:Long ) = deadbolt.Restrict(allOfGroup(UserRole.Campaigner.toString))() { implicit req =>
    logger.info(s"Taking down campaign $id")
    campaigns.isAllowedToManage(req.subject.get.asInstanceOf[HearUsSubject], id).flatMap(ans => {
      if (ans) {
        uncacheActor.tell(InvalidateCampaignById(id, frontPageOnly=false), ActorRef.noSender )
        campaigns.updateStatus(id, CampaignStatus.WorkInProgress).map( c=>Ok("updated") )
      } else {
        Future(Unauthorized("Permission Denied"))
      }
    })
  }
}
