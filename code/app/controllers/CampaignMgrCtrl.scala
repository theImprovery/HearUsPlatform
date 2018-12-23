package controllers

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions, allOfGroup}
import dataaccess._
import javax.inject.Inject

import models.{Position, UserRole}
import play.api.Configuration
import play.api.i18n.{Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController, Result}
import security.HearUsSubject

import scala.concurrent.Future

class CampaignMgrCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                campaigns:CampaignDAO, users:UsersDAO, usersCampaigns:UserCampaignDAO,
                                langs:Langs, messagesApi:MessagesApi, images: ImagesDAO,
                                conf:Configuration, ws:WSClient) extends InjectedController {
  
  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  
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
