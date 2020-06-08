package controllers

import java.util.concurrent.TimeUnit

import be.objectify.deadbolt.scala.DeadboltActions
import be.objectify.deadbolt.scala.models.Subject
import dataaccess.{CampaignDAO, ImagesDAO, KmGroupDAO, KnessetMemberDAO, UserCampaignDAO, UserDAO}
import javax.inject.Inject
import models.{Campaign, CampaignStatus, CannedMessage, ContactOption, Platform, Position, UserRole}
import play.api.cache.Cached
import play.api.{Configuration, Logger}
import play.api.i18n.{I18nSupport, Langs, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents, InjectedController}
import security.{HearUsRole, HearUsSubject}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

object CampaignPublicCtrl {
  def campaignCacheKey(slug:String) = s"CampaignPublicCtrl/campaign/$slug"
  def kmCacheKey(slug:String, mkId:Long) = s"CampaignPublicCtrl/campaign/$slug/$mkId"
}

class CampaignPublicCtrl @Inject()(cc:ControllerComponents, kms:KnessetMemberDAO,
                                   images: ImagesDAO, groups:KmGroupDAO,
                                   campaigns:CampaignDAO, userCampaigns:UserCampaignDAO, messagesApi:MessagesApi, deadbolt:DeadboltActions,
                                   conf:Configuration, cached:Cached) extends AbstractController(cc) with I18nSupport {
  import CampaignPublicCtrl._
  implicit private val ec = controllerComponents.executionContext
  val logger = Logger(this.getClass)
  
  def index( campaignSlug:String ) = cached(_=>campaignCacheKey(campaignSlug), Duration(30, TimeUnit.SECONDS) ) {
    deadbolt.WithAuthRequest()(){ implicit req =>
      campaigns.getBySlug(campaignSlug).flatMap({
        case None => Future(NotFound( views.html.errorPage(404, messagesApi.preferred(req)("errors.campaignNotFound"))))
        case Some(camp) => canView( req.subject, camp ).flatMap({
          case false => Future(Unauthorized( views.html.errorPage(401, messagesApi.preferred(req)("errors.userNotAuthorizedToSeeCampaign"))))
          case true => for {
              texts <- campaigns.getTextsFor(camp.id)
              kmsSeq  <- kms.getAllActiveKms().map( Random.shuffle(_) )
              dbPositions <- campaigns.getPositions(camp.id).map( posSeq => posSeq.map( p => (p.kmId, p.position)).toMap )
              kmImages <- images.getAllKmImages
              bkgPrefix = conf.get[String]("hearUs.files.campaignImages.url")
              campaignImage <- images.getImageForCampaign(camp.id).map( opt => opt.map(_=>bkgPrefix + camp.id) )
              parties <- kms.getAllActiveParties().map( _.sortBy(_.name) )
              committees <- groups.getGroupsForCampaign(camp.id).map(_.sortBy(_.name))
              memberships <- groups.groupsMemberships( committees.map(_.id).toSet )
            } yield {
              val partyMap = parties.map(p=>(p.id,p)).toMap
              val km2Party = kmsSeq.map( km => (km.id, partyMap.get(km.partyId)))
                .filter( _._2.isDefined)
                .map( p => (p._1, p._2.get)).toMap
              val imagePrefix = conf.get[String]("hearUs.files.mkImages.url")
              val km2Image = kmsSeq.map( km => (km.id, kmImages.get(km.id).map( _ => imagePrefix + km.id ).getOrElse("/assets/images/kmNoImage.jpg")) ).toMap
              val km2Position = kmsSeq.map( km => (km.id, dbPositions.getOrElse(km.id, Position.Undecided))).toMap
              val km2Cmt = memberships.map( kv => kv._2.map(km =>(km,kv._1)) ).flatten.groupBy(_._1).map( kv => (kv._1, kv._2.map(_._2).toSet))
              Ok( views.html.campaignPublic.campaignFrontPage(camp, campaignImage, texts.get, kmsSeq, parties, committees,
                  km2Party, km2Image, km2Position, km2Cmt, conf.getOptional[String]("hearUs.analytics")) )
  }})})}}
  
  def kmPage( campaignSlug:String, kmId:Long ) =  cached(_=>kmCacheKey(campaignSlug, kmId), 3.minutes) {
    deadbolt.WithAuthRequest()(){ implicit req =>
      campaigns.getBySlug(campaignSlug).flatMap({
        case None => Future(NotFound( views.html.errorPage(404, messagesApi.preferred(req)("errors.campaignNotFound"))))
        case Some(camp) => canView( req.subject, camp ).flatMap({
          case false =>  Future(Unauthorized( views.html.errorPage(401, messagesApi.preferred(req)("errors.userNotAuthorizedToSeeCampaign"))))
          case true  => for {
            texts      <- campaigns.getTextsFor(camp.id)
            kmOpt      <- kms.getKM(kmId)
            party      <- kmOpt.map( km=>kms.getParty(km.partyId) ).getOrElse(Future(None))
            actions    <- campaigns.getActions(camp.id, kmId)
            kmPosition <- campaigns.getPosition(camp.id, kmId)
            effPosition = kmPosition.map( _.position).getOrElse(Position.Undecided)
            kmContact  <- kms.getContactOptions(kmId).map( _.filter( _.platformObj.isDefined).groupBy(_.platformObj).map(e=>(e._1.get,e._2)) )
            imagePrefix = conf.get[String]("hearUs.files.mkImages.url")
            bkgPrefix = conf.get[String]("hearUs.files.campaignImages.url")
            kmImageOpt <- images.getImageForKm(kmId)
            campaignImage <- images.getImageForCampaign(camp.id).map( opt => opt.map(_=>bkgPrefix + camp.id) )
            kmImageUrl    = kmImageOpt.map( _ => imagePrefix + kmId ).getOrElse("/assets/images/kmNoImage.jpg")
            kmImageCredit = kmImageOpt.map( _.credit )
            cannedMessages <- kmOpt.map( km=>campaigns.getMessage(camp.id,km.genderVal, effPosition)).getOrElse(Future(Map[Platform.Value, CannedMessage]()))
          } yield {
            kmOpt match {
              case Some(km) => {
                val twitterHandleOpt = kmContact.get(Platform.Twitter).flatMap(_.headOption).map(_.details)
                val emailMessageOpt = cannedMessages.get(Platform.Email).map( _.process(km, twitterHandleOpt))
                val twitterMessageMap = cannedMessages.get(Platform.Twitter) match {
                  case None => Map[ContactOption, String]()
                  case Some( cndMsg ) => {
                    kmContact.getOrElse(Platform.Twitter, Seq[ContactOption]()).map(hdl =>
                      ( hdl -> cndMsg.process(km, Some(hdl.details)).text )
                    ).toMap
                  }
                }
                val whatsAppCndMsgMap = cannedMessages.get(Platform.WhatsApp) match {
                  case None => Map[ContactOption, String]()
                  case Some(cndMsg) => {
                    val contOpt = (kmContact.get(Platform.WhatsApp)++kmContact.get(Platform.PhoneAndWhatsApp)).flatten
                    contOpt.map( c => c-> cndMsg.process(km, Some(c.details)).text ).toMap
                  }
                }
                Ok(views.html.campaignPublic.campaignKMPage(camp, campaignImage, texts.get, km, party,
                  effPosition, kmImageUrl, kmImageCredit, actions,
                  kmContact -- Set(Platform.Phone, Platform.Fax),
                  emailMessageOpt, twitterMessageMap, whatsAppCndMsgMap,
                  conf.getOptional[String]("hearUs.analytics")))
              }
              case None => NotFound( views.html.errorPage(404, messagesApi.preferred(req)("errors.campaignNotFound")))
            }
          }
        })
      })
  }}
  
  
  val roleAdmin = HearUsRole( UserRole.Admin.toString )
  val roleCampaigner = HearUsRole( UserRole.Campaigner.toString )
  
  def canView( subject:Option[Subject], campaign:Campaign ):Future[Boolean] = {
    
    // easy: campaign is published.
    if ( campaign.status == CampaignStatus.Published ) return Future(true)
    
    subject match {
      case None => Future(false) // not logged in -> can't see
      case Some( subj ) => {
        val hus = subj.asInstanceOf[HearUsSubject]
        if ( hus.roles.contains(roleAdmin) ) return Future(true) // site admins can see all
        if ( hus.roles.contains(roleCampaigner) ) {
          userCampaigns.isUserOnCampaign( hus.user.id, campaign.id ) // campaigners can see their campaign
        } else {
          Future(false) // campaigners can't see other campaigns
        }
      }
    }
  }
  
}
