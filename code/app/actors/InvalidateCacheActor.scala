package actors

import akka.actor.Actor
import akka.util.Timeout
import controllers.CampaignPublicCtrl
import dataaccess.{CampaignDAO, KnessetMemberDAO}
import javax.inject.Inject
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.mvc.ControllerComponents

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext


object InvalidateCacheActor {
  case class InvalidateCampaignById(id:Long, frontPageOnly:Boolean)
  case class InvalidateCampaignBySlug(slug:String, frontPageOnly:Boolean)
  case class InvalidateKM(id:Long)
  case class InvalidateKmInCampaign(campId:Long, mkId:Long)
  case class InvalidateCampaigns(frontPageOnly:Boolean)
}

class InvalidateCacheActor @Inject() (anEx:ExecutionContext, campaigns:CampaignDAO,
                                      kms:KnessetMemberDAO, cache:AsyncCacheApi) extends Actor {
  import InvalidateCacheActor._
  
  implicit val timeout:Timeout = Timeout(6.seconds)
  implicit val ec:ExecutionContext = anEx
  private val logger = Logger(classOf[InvalidateCacheActor])
  
  override def receive: Receive = {
    case InvalidateCampaignById(id, fpo) => {
      campaigns.getSlug(id).map({
        case None => ()
        case Some(slg) => {
          invalidateCampaign(slg, fpo)
        }
      })
    }
    
    case InvalidateCampaignBySlug(slg, fpo) => invalidateCampaign(slg, fpo)
    
    case InvalidateKM(id) => {
      campaigns.getCampaignSlugs.foreach(_.foreach( slg => cache.remove(CampaignPublicCtrl.kmCacheKey(slg,id))))
    }
    
    case InvalidateKmInCampaign(campId, kmId) => {
      campaigns.getSlug(campId).map( {
        case None => ()
        case Some(slg) => {
          cache.remove(CampaignPublicCtrl.campaignCacheKey(slg))
          cache.remove(CampaignPublicCtrl.kmCacheKey(slg, kmId))
        }
      })
    }
    
    case InvalidateCampaigns(frontPageOnly) => {
      campaigns.getCampaignSlugs.foreach(_.foreach( slg => self ! invalidateCampaign(slg, frontPageOnly=frontPageOnly)))
    }
  }
  
  def invalidateCampaign( slug:String, frontPageOnly:Boolean ):Unit = {
    cache.remove(CampaignPublicCtrl.campaignCacheKey(slug))
    if ( ! frontPageOnly ) {
      kms.getAllActiveKmIds.foreach(_.foreach(kmId=>cache.remove(CampaignPublicCtrl.kmCacheKey(slug, kmId))))
      
    }
    
  }
}
