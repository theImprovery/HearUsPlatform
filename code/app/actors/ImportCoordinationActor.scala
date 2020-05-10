package actors

import actors.ImportCommitteesActor._
import actors.ImportCoordinationActor._
import actors.ImportSinglePageActor._
import akka.actor.{Actor, ActorRef, Props}
import javax.inject.{Inject, Named}
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import akka.util.Timeout
import dataaccess.{KmGroupDAO, KnessetMemberDAO}
import models.{ContactOption, KmGroup, KnessetMember, Party}
import play.api.{Configuration, Logger}
import play.api.i18n._

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.xml.NodeSeq

object ImportCoordinationActor {
  def props = Props[ImportCoordinationActor]
  case class ImportAll(kmsFirstPage:String, partiesFirstPage:String, numPage:String, ptpPage:String)
  case class LoadKmPage(nextPage:String)
  case class LoadPartyPage(nextPage:String)
  case class LoadPsn2FcnPage(nextPage:String)
  case class AddParties(newParties:Map[String,String])
  case class AddKms(newKms:Set[KnessetMember], mappedEmails:Map[String, String])
  case class GetKnessetNum(page:String)
  case class AddPsn2Fcn(newPtF:Map[String, String]) // Person to faction
}

class ImportCoordinationActor @Inject()(ws:WSClient, knessetMemberStore: KnessetMemberDAO,
                                        langs:Langs, messagesApi:MessagesApi,
                                        @Named("committee-actor") committeeImportActor:ActorRef,
                                        ec:ExecutionContext, conf:Configuration)
  extends BaseKnessetActor(ec, ws) {
  val logger:Logger = Logger(classOf[ImportCoordinationActor])

  private var activeKmQueries = 0
  private var activePartyQueries = 0
  private var activePsn2FcnQueries = 0
  private var knessetNum = 0
  private val kms: scala.collection.mutable.Set[KnessetMember] = scala.collection.mutable.Set()
  private val parties: scala.collection.mutable.Set[Party] = scala.collection.mutable.Set()
  private var partyKeyMap: Map[String, String] = Map()
  private var personToFaction: Map[String, String] = Map()
  private var personToMail:Map[String, String] = Map()
  
  override def receive: Receive = {
    case ImportAll(kmsFirstPage:String, partiesFirstPage:String, numPage:String, ptpPage:String) => {
      logger.info("Starting full Knesset Import")
      get(numPage, res => {
        val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
        val currentK = properties.filter(node => (node\"IsCurrent").text.toBoolean).head
        knessetNum = (currentK \ "KnessetNum").text.toInt
        self ! LoadKmPage(kmsFirstPage)
        self ! LoadPartyPage(partiesFirstPage)
        self ! LoadPsn2FcnPage(ptpPage)
        logger.info( s"Import of Knesset #$knessetNum initiated")
      })
    }
    case LoadKmPage(nextPage:String) => {
      activeKmQueries += 1
      children.route( ImportSingleKmPage(nextPage, knessetNum, self), self )
    }
    case LoadPartyPage(nextPage:String) => {
      activePartyQueries += 1
      children.route( ImportSinglePartyPage(nextPage, knessetNum, self), self )
    }
    case LoadPsn2FcnPage(nextPage:String) => {
      activePsn2FcnQueries += 1
      children.route( ImportSinglePerson2FactionPage(nextPage, self), self )
    }
    
    case AddParties(newParties:Map[String,String]) => {
      partyKeyMap ++= newParties
      parties ++= newParties.map( tupl => Party(-1L, tupl._2, "", isActive=true))
      activePartyQueries -= 1
      possiblyFinish()
    }
    
    case AddKms(newKms:Set[KnessetMember], mappedEmails:Map[String, String]) => {
      activeKmQueries -= 1
      kms ++= newKms
      personToMail ++= mappedEmails
      possiblyFinish()
    }
    
    case AddPsn2Fcn(newPtF:Map[String, String]) => {
      activePsn2FcnQueries -= 1
      personToFaction ++= newPtF
      possiblyFinish()
    }
    
  }

  private def possiblyFinish():Unit = {
    if ( activePartyQueries == 0 && activeKmQueries == 0 && activePsn2FcnQueries == 0 ) {
      updateAll()
    }
  }
  
  private def updateAll() = {
    implicit val ecc:ExecutionContext = ec
    implicit val messagesProvider: MessagesProvider = {
      MessagesImpl(langs.availables.head, messagesApi)
    }
    
    val kmsWithFaction = kms.filter(km => personToFaction.contains(km.knessetKey.toString))
    logger.info( s"KM # in Knesset: ${kms.size} / ${kmsWithFaction.size}")
    kmsWithFaction.toSeq.zipWithIndex.foreach( kmt => logger.info(s"(${kmt._2}) ${kmt._1.knessetKey} ${kmt._1.name}"))
    
    logger.info("======")
    logger.info("Parties:")
    parties.toSeq.zipWithIndex.foreach(pt => logger.info(s"(${pt._2}) ${pt._1.name} ${pt._1.isActive}"))
    for {
        // update parties and factions
      updatedParties <- knessetMemberStore.updateParties(parties.toSeq)
      activeParties  <- knessetMemberStore.getAllActiveParties()
  
      // update knesset members
      allContactOptions <- knessetMemberStore.getAllContactOptions()
      partyNameToID  = activeParties.map(party => (party.name, party.id)).toMap
      kmToContactOpt = allContactOptions.groupBy(_.kmId.get)
      kmsWithParty = kmsWithFaction.map(km => {
          val partyId = partyNameToID(personToFaction(km.knessetKey.toString))
          km.copy(partyId = partyId)
        })
      _  <- knessetMemberStore.updateKms(kmsWithParty.toSeq)
      currentKms <- knessetMemberStore.getAllActiveKms()
    } yield {
      val knessetKeyToPerson = currentKms.map( ck => ( ck.knessetKey, ck.id )).toMap
      personToMail = personToMail.filter( p => knessetKeyToPerson.contains(p._1.toLong) )

      //Check for duplicate ContactOption from KnessetApi and add if non exists
      val newContacts:Set[ContactOption] = personToMail.filter( ptm => {
        !kmToContactOpt.getOrElse(knessetKeyToPerson(ptm._1.toLong), Seq())
          .exists(co => (co.platform == "Email" && co.title == Messages("platform.email.title")
            && co.details == ptm._2 && co.note == Messages("platform.email.note")))
      }).map(ptm => ContactOption( 0, Some(knessetKeyToPerson(ptm._1.toLong)), None, "Email",
          Messages("platform.email.title"), ptm._2, Messages("platform.email.note"))).toSet
      
      knessetMemberStore.addContactOption(newContacts).map(_ => {
        // MKs done, import committees.
        logger.info("Knesset members and factions imported. Moving on to committees.")
        committeeImportActor ! ImportCommittees(
          conf.get[String]("xml.committees"),
          conf.get[String]("xml.ptpCommittees"),
          conf.get[String]("xml.knessetDates"))
      })
    }
  }
}







