package controllers

import dataaccess.{CampaignDAO, ImagesDAO, KnessetMemberDAO}
import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.{Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, InjectedController}

class CampaignPublicCtrl @Inject()(cc:ControllerComponents, kms:KnessetMemberDAO,
                                   campaigns:CampaignDAO,
                                   langs:Langs, messagesApi:MessagesApi, images: ImagesDAO,
                                   conf:Configuration, ws:WSClient) extends InjectedController {
  
  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  
  
  def index( campaignSlug:String ) = Action{
    Ok("Showing campaign: " + campaignSlug )
  }
  
  
}
