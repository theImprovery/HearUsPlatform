package actors

import actors.EmailSendingActor.{NewCampaignStarted, OffensiveContentReport, PublicationRequest}
import akka.actor.Actor
import dataaccess.{CampaignDAO, KnessetMemberDAO, UserDAO}
import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.cache.AsyncCacheApi
import play.api.libs.mailer.{Email, MailerClient}
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

object EmailSendingActor {
  case class NewCampaignStarted(campaignId:Long)
  case class PublicationRequest(campaignId:Long)
  case class OffensiveContentReport(campaignId:Long, url:String, report:String)
}

/**
  * An actor responsible for sending emails out.
  */
class EmailSendingActor @Inject() (anEx:ExecutionContext, campaigns:CampaignDAO, users:UserDAO,
                                   mailer:MailerClient, cfg:Configuration) extends Actor {
  
  implicit val ec:ExecutionContext = anEx
  private val logger = Logger(classOf[EmailSendingActor])
  
  override def receive: Receive = {
    case NewCampaignStarted(camId) => {
      for {
        c <- campaigns.getCampaign(camId)
        mgrs <- campaigns.getCampaignManagers(camId)
      } yield {
        c match {
          case Some(camp) => {
            val htmlContent = views.html.emailTemplates.newCampaignStarted(camp, mgrs, cfg.get[String]("psps.server.publicUrl") ).body
            mailer.send(new Email(
              "[Hear-Us] New campaign created", "noreply@hear-us.org.il", Seq(cfg.get[String]("hearUs.adminEmail")),
              None, Some(htmlContent), Some("utf-8"))
            )
          }
          case None => logger.warn(s"Got a message about nonexistent new campaign, with id=$camId")
        }
      }
    }

    case PublicationRequest(camId) => {
      for {
        c <- campaigns.getCampaign(camId)
        mgrs <- campaigns.getCampaignManagers(camId)
      } yield {
        c match {
          case Some(camp) => {
            val htmlContent = views.html.emailTemplates.campaignPublicationRequest(camp, mgrs, cfg.get[String]("psps.server.publicUrl") ).body
            mailer.send(new Email(
              "[Hear-Us] A campaign wants to become public", "noreply@hear-us.org.il", Seq(cfg.get[String]("hearUs.adminEmail")),
              None, Some(htmlContent), Some("utf-8"))
            )
          }
          case None => logger.warn(s"Got a message about nonexistent new campaign, with id=$camId")
        }
      }
    }

    case OffensiveContentReport(campaignId, url, report) => {
      for {
        c <- campaigns.getCampaign(campaignId)
        mgrs <- campaigns.getCampaignManagers(campaignId)
      } yield {
        c match {
          case Some(camp) => {
            val htmlContent = views.html.emailTemplates.abusiveContentReport(camp, mgrs, url, report).body
            mailer.send(new Email(
              s"[Hear-Us] Offensive content report about campaign ${camp.title}", "noreply@hear-us.org.il", Seq(cfg.get[String]("hearUs.adminEmail")),
              None, Some(htmlContent), Some("utf-8"))
            )
          }
          case None => logger.warn(s"Got a message about nonexistent new campaign, with id=$campaignId")
        }
      }
    }
  }
}
