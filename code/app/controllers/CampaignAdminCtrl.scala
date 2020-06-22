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
import play.api.cache.AsyncCacheApi
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

class CampaignAdminCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents,
                                  users:UserDAO, campaigns:CampaignDAO, cache:AsyncCacheApi,
                                  userCampaigns:UserCampaignDAO, settings: SettingsDAO,
                                  langs:Langs, messagesApi:MessagesApi, conf:Configuration) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit private val logger = Logger(classOf[CampaignAdminCtrl])
  implicit private val cnf:Configuration = conf
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  
  def showCampaigns = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    val userId = req.asInstanceOf[AuthenticatedRequest[_]].subject.get.asInstanceOf[HearUsSubject].user.id
    for {
      camps <- campaigns.getAllCampaigns
      contacts <- campaigns.getCampaignContact
      myCampaignIds <- userCampaigns.getCampaignsForUser(userId).map( _._1.map(_.id).toSet )
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
  
  def showFrontPageEditor() = deadbolt.Restrict( allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    for {
      fpsOpt <- settings.get(SettingKey.HOME_PAGE_TEXT)
      pageText = fpsOpt.map(_.value).getOrElse("HearUs: Front page text")
    } yield {
      Ok( views.html.campaignAdmin.frontPageEditor(pageText))
    }
  }
  
  def apiPutFrontPageData() = deadbolt.Restrict( allOfGroup(UserRole.Admin.toString))(cc.parsers.byteString) { req =>
    val payload = req.body.decodeString("UTF-8")
    val stg = Setting( SettingKey.HOME_PAGE_TEXT, payload )
    for {
      _ <- settings.store(stg)
    } yield {
      cache.remove(SettingKey.HOME_PAGE_TEXT.toString)
      logger.info("Home page text removed")
      Ok("updated")
    }
  }
  
  private def campaignEditorAction(camId:Long)(action:Future[Result])(implicit req:AuthenticatedRequest[_]) = {
    campaigns.isAllowedToEdit(req.subject.get.asInstanceOf[HearUsSubject], camId).flatMap(ans => {
      if(ans) {
        action
      } else {
        Future(Unauthorized("Permission Denied"))
      }
    })
  }

}
