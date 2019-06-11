package controllers

import dataaccess.{CampaignDAO, ImagesDAO, KmGroupDAO, KnessetMemberDAO}
import javax.inject.Inject
import models.Position
import play.api.{Configuration, Logger}
import play.api.i18n.{I18nSupport, Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents, InjectedController}

import scala.concurrent.Future

class CampaignPublicCtrl @Inject()(cc:ControllerComponents, kms:KnessetMemberDAO, campaigns:CampaignDAO,
                                   images: ImagesDAO, groups:KmGroupDAO,
                                   langs:Langs, messagesApi:MessagesApi,
                                   conf:Configuration, ws:WSClient) extends AbstractController(cc) with I18nSupport {
  
  implicit private val ec = controllerComponents.executionContext
  val logger = Logger(this.getClass)
  
  def index( campaignSlug:String ) = Action.async{ implicit req =>
    for {
      campOpt <- campaigns.getBySlug(campaignSlug)
      texts <- campOpt.map( c => campaigns.getTextsFor(c.id) ).getOrElse(Future(None))
      kmsSeq  <- kms.getAllActiveKms().map( _.sortBy(_.name) )
      campId = campOpt.map( _.id ).getOrElse(-1l)
      dbPositions <- campaigns.getPositions(campId).map( posSeq => posSeq.map( p => (p.kmId, p.position)).toMap )
      kmImages <- images.getAllKmImages
      parties <- kms.getAllActiveParties().map( _.sortBy(_.name) )
      committees <- groups.getGroupsForCampaign(campId).map(_.sortBy(_.name))
      memberships <- groups.groupsMemberships( committees.map(_.id).toSet )
    } yield {
      campOpt match {
        case None => NotFound( views.html.errorPage(404, messagesApi.preferred(req)("errors.campaignNotFound")))
        case Some( c ) => {
          val partyMap = parties.map(p=>(p.id,p)).toMap
          val km2Party = kmsSeq.map( km => (km.id, partyMap.get(km.partyId)))
                               .filter( _._2.isDefined)
                               .map( p => (p._1, p._2.get)).toMap
          val imagePrefix = conf.get[String]("hearUs.files.mkImages.url")
          
          val km2Image = kmsSeq.map( km => (km.id,
                                            kmImages.get(km.id).map( imagePrefix + _.filename ).getOrElse("/assets/images/kmNoImage.jpg")
                                           ) ).toMap
          val km2Position = kmsSeq.map( km => (km.id, dbPositions.getOrElse(km.id, Position.Undecided))).toMap
          val km2Cmt = memberships.map( kv => kv._2.map(km =>(km,kv._1)) ).flatten.groupBy(_._1).map( kv => (kv._1, kv._2.map(_._2).toSet))
          Ok( views.html.campaignPublic.campaignFrontPage(c, texts.get, kmsSeq, parties, committees, km2Party, km2Image, km2Position, km2Cmt) )
        }
      }
    }
  }
  
  
}
