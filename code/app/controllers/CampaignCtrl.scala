package controllers

import be.objectify.deadbolt.scala.DeadboltActions
import dataaccess.{CampaignDAO, ImagesDAO, KmGroupDAO, KnessetMemberDAO}
import javax.inject.Inject
import models.Campaign
import play.api.Configuration
import play.api.i18n.{Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CampaignCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                             campaigns:CampaignDAO, images: ImagesDAO, groups: KmGroupDAO, langs:Langs, messagesApi:MessagesApi,
                             conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  def showCampaigns() = deadbolt.SubjectPresent()() { implicit req =>
    for {
      camps <- campaigns.getAllCampaigns
    } yield Ok(views.html.knesset.campaigns(camps))
  }

  def createCampaign() = deadbolt.SubjectPresent()() { implicit req =>
    val newCamp = Campaign(-1L, "", "", "", "", "")
    campaigns.add(newCamp).map(stored => Created(routes.CampaignCtrl.editCampaign( stored.id ).url))
  }

  def editCampaign(camId:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for {
      camps <- campaigns.getAllCampaigns
    } yield Ok(views.html.knesset.campaigns(camps))
  }

}
