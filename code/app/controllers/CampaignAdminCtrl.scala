package controllers

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions, allOfGroup}
import dataaccess._
import javax.inject.Inject
import models.{CampaignStatus, _}
import play.api.{Configuration, Logger}
import play.api.i18n.{Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController, Result}
import dataaccess.JSONFormats._
import security.HearUsSubject

import scala.concurrent.Future

object CampaignAdminCtrl {
  val statusSortOrder: Map[CampaignStatus.Value, String] = Map(
    CampaignStatus.PublicationRequested -> "0",
    CampaignStatus.Published            -> "1",
    CampaignStatus.WorkInProgress       -> "2",
    CampaignStatus.Rejected             -> "3"
  )
}

class CampaignAdminCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                  users:UserDAO, campaigns:CampaignDAO, images: ImagesDAO, groups: KmGroupDAO,
                                  userCampaigns:UserCampaignDAO,
                                  langs:Langs, messagesApi:MessagesApi, conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit private val logger = Logger(classOf[CampaignAdminCtrl])
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  
  def showCampaigns = deadbolt.SubjectPresent()() { implicit req =>
    val userId = req.asInstanceOf[AuthenticatedRequest[_]].subject.get.asInstanceOf[HearUsSubject].user.id
    for {
      camps <- campaigns.getAllCampaigns
      contacts <- campaigns.getCampaignContact
      myCampaignIds <- userCampaigns.getCampaginsForUser(userId).map( _.map(_.id).toSet )
    } yield Ok(views.html.campaignAdmin.allCampaigns(camps.sortBy(c => CampaignAdminCtrl.statusSortOrder(c.status) + c.title), contacts, myCampaignIds))
  }

  def getCampaigners(searchStr:String) = deadbolt.SubjectPresent()() { implicit req =>
    val sqlSearch = "%"+searchStr.trim+"%"
    users.allCampaigners(Some(sqlSearch)).map( ans => Ok(Json.toJson(ans.map(_.dn))))
  }
  
  def deleteCampaign(id:Long, from:String) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    for {
      deleted <- campaigns.deleteCampaign(id)
    } yield {
      val goTo = from match {
        case "admin" => routes.CampaignAdminCtrl.showCampaigns().url
        case "campaigner" => routes.CampaignMgrCtrl.index().url
      }
      Ok(goTo).flashing(FlashKeys.MESSAGE -> messagesProvider.messages("campaigns.deleted"))
    }
  }

  def updateAction = deadbolt.SubjectPresent()()  { implicit req =>
    Future(Ok("IMPL"))
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
