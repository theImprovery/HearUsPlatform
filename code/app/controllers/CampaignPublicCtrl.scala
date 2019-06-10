package controllers

import dataaccess.{CampaignDAO, ImagesDAO, KnessetMemberDAO}
import javax.inject.Inject
import models.Position
import play.api.Configuration
import play.api.i18n.{I18nSupport, Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents, InjectedController}

import scala.concurrent.Future

class CampaignPublicCtrl @Inject()(cc:ControllerComponents, kms:KnessetMemberDAO, campaigns:CampaignDAO,
                                   langs:Langs, messagesApi:MessagesApi, images: ImagesDAO,
                                   conf:Configuration, ws:WSClient) extends AbstractController(cc) with I18nSupport {
  
  implicit private val ec = controllerComponents.executionContext
  
  
  def index( campaignSlug:String ) = Action.async{ implicit req =>
    for {
      campOpt <- campaigns.getBySlug(campaignSlug)
      texts <- campOpt.map( c => campaigns.getTextsFor(c.id) ).getOrElse(Future(None))
      kmsSeq  <- kms.getAllKms.map( _.sortBy(_.name) )
      parties <- kms.getAllParties.map( pSeq => pSeq.map( p=>(p.id, p) ).toMap )
      campId = campOpt.map( _.id ).getOrElse(-1l)
      dbPositions <- campaigns.getPositions(campId).map( posSeq => posSeq.map( p => (p.kmId, p.position)).toMap )
      kmImages <- images.getAllKmImages
    } yield {
      campOpt match {
        case None => NotFound( views.html.errorPage(404, messagesApi.preferred(req)("errors.campaignNotFound")))
        case Some( c ) => {
          val km2Party = kmsSeq.map( km => (km.id, parties.get(km.partyId)))
                               .filter( _._2.isDefined)
                               .map( p => (p._1, p._2.get)).toMap
          val imagePrefix = conf.get[String]("hearUs.files.mkImages.url")
          
          val km2Image = kmsSeq.map( km => (km.id,
                                            kmImages.get(km.id).map( imagePrefix + _.filename ).getOrElse("/assets/images/kmNoImage.jpg")
                                           ) ).toMap
          val km2Position = kmsSeq.map( km => (km.id, dbPositions.getOrElse(km.id, Position.Undecided))).toMap
          Ok( views.html.campaignPublic.campaignFrontPage(c, texts.get, kmsSeq, km2Party, km2Image, km2Position) )
        }
      }
    }
  }
  
  
}
