package actors

import actors.ImportCommitteesActor._
import actors.ImportCoordinationActor._
import actors.ImportSinglePageActor._
import akka.actor.{Actor, ActorRef, Props}
import javax.inject.Inject
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import akka.util.Timeout
import dataaccess.{KmGroupDAO, KnessetMemberDAO}
import models.{ContactOption, KmGroups, KnessetMember, Party}
import play.api.Logger
import play.api.i18n._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.xml.NodeSeq

object ImportCoordinationActor {
  def props = Props[ImportCoordinationActor]
  case class importAll(kmsFirstPage:String, partiesFirstPage:String, numPage:String, ptpPage:String)
  case class loadKmNextPage(nextPage:String)
  case class loadPartyNextPage(nextPage:String)
  case class loadPtFNextPage(nextPage:String)
  case class newParties(newParties:Map[String,String])
  case class newKms(newKms:Set[KnessetMember], mappedEmails:Map[String, String])
  case class partiesIspaFinish(finished:ActorRef)
  case class kmIspaFinish(finished:ActorRef)
  case class ptfIspaFinish(finished:ActorRef)
  case class getKnessetNum(page:String)
  case class newPersonToFaction(newPtF:Map[String, String])
}

class ImportCoordinationActor @Inject()(ws:WSClient, cc:ControllerComponents, knessetMembers: KnessetMemberDAO,
                                        langs:Langs, messagesApi:MessagesApi) extends Actor {
  implicit val timeout = Timeout(20 seconds)
  private var kmsCount = 0
  private var partiesCount = 0
  private var ptfCount = 0
  private var children: scala.collection.mutable.Set[ActorRef] = scala.collection.mutable.Set()
  private var kms: scala.collection.mutable.Set[KnessetMember] = scala.collection.mutable.Set()
  private var parties: scala.collection.mutable.Set[Party] = scala.collection.mutable.Set()
  private var partyKeyMap: Map[String, String] = Map()
  private var personToFaction: Map[String, String] = Map()
  private var personToMail:Map[String, String] = Map()
  private var knessetNum = 0
  override def receive: Receive = {
    case importAll(kmsFirstPage:String, partiesFirstPage:String, numPage:String, ptpPage:String) => {
      implicit val ec = cc.executionContext
      ws.url(numPage)
        .withFollowRedirects(true)
        .get()
        .map(res => {
          val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
          val currentK = properties.filter(node => (node \ "IsCurrent" text).toBoolean).head
          knessetNum = (currentK \ "KnessetNum" text).toInt
          kmsCount += 1
          partiesCount += 1
          ptfCount += 1
          //Start load kms
          var child = context.actorOf(ImportSinglePageActor.props)
          child ! importKmSinglePage(kmsFirstPage, knessetNum, ws, cc, self)
          children = children + child
          //Start load parties
          child = context.actorOf(ImportSinglePageActor.props)
          child ! importPartiesSinglePage(partiesFirstPage, knessetNum, ws, cc, self)
          children = children + child
          //Start load person to faction
          child = context.actorOf(ImportSinglePageActor.props)
          child ! importPtoFSinglePage(ptpPage, ws, cc, self)
          children = children + child
        })
    }
    case loadKmNextPage(nextPage:String) => {
      kmsCount += 1
      val child = context.actorOf(ImportSinglePageActor.props)
      child ! importKmSinglePage(nextPage, knessetNum, ws, cc, self)
      children += child
    }
    case loadPartyNextPage(nextPage:String) => {
      partiesCount += 1
      val child = context.actorOf(ImportSinglePageActor.props)
      child ! importPartiesSinglePage(nextPage, knessetNum, ws, cc, self)
      children += child
    }
    case loadPtFNextPage(nextPage:String) => {
      ptfCount += 1
      val child = context.actorOf(ImportSinglePageActor.props)
      child ! importPtoFSinglePage(nextPage, ws, cc, self)
      children += child
    }
    case newParties(newParties:Map[String,String]) => {
      partyKeyMap ++= newParties
      parties ++= newParties.map( tupl => Party(-1L, tupl._2, "", true))
    }
    case newKms(newKms:Set[KnessetMember], mappedEmails:Map[String, String]) => {
      kms ++= newKms
      personToMail ++= mappedEmails
    }
    case newPersonToFaction(newPtF:Map[String, String]) => {
      personToFaction ++= newPtF
    }
    case kmIspaFinish(finished:ActorRef) => {
      context.stop(finished)
      kmsCount -= 1
      if( partiesCount == 0 & kmsCount == 0 & ptfCount == 0){
        updateAll()
      }
    }
    case partiesIspaFinish(finished:ActorRef) => {
      context.stop(finished)
      partiesCount -= 1
      if( partiesCount == 0 & kmsCount == 0 & ptfCount == 0){
        updateAll()
      }
    }
    case ptfIspaFinish(finished:ActorRef) => {
      context.stop(finished)
      ptfCount -= 1
      if( partiesCount == 0 & kmsCount == 0 & ptfCount == 0){
        updateAll()
      }
    }
  }

  private def updateAll() = {
    implicit val messagesProvider: MessagesProvider = {
      MessagesImpl(langs.availables.head, messagesApi)
    }
    kms = kms.filter(km => personToFaction.contains(km.knessetKey.toString))
    implicit val ec = cc.executionContext
    for {
        updatedParties <- knessetMembers.updateParties(parties.toSeq)
        activeParties  <- knessetMembers.getAllActiveParties()
        partyNameToID  = activeParties.map(party => (party.name, party.id)).toMap
        mappedKms = kms.map(km => {
          val partyId = partyNameToID(personToFaction(km.knessetKey.toString))
          KnessetMember(km.id, km.name, km.gender, km.isActive, km.webPage, partyId, km.knessetKey)
        })
        updatedKm      <- knessetMembers.updateKms(mappedKms.toSeq)
        currentKms <- knessetMembers.getAllActiveKms()
    } yield {
      val knessetKeyToPerson = currentKms.map( ck => ( ck.knessetKey, ck.id )).toMap
      personToMail = personToMail.filter( p => knessetKeyToPerson.contains(p._1.toLong) )
      val newContacts = personToMail.map( ptm => ContactOption( knessetKeyToPerson(ptm._1.toLong), "Email",
                                                                Messages("platform.email.title"), ptm._2, Messages("platform.email.note") )).toSeq
      knessetMembers.addContactOption(newContacts)
    }
  }
}

object ImportSinglePageActor {
  def props = Props[ImportSinglePageActor]
  case class importKmSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, actor:ActorRef)
  case class importPartiesSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, actor:ActorRef)
  case class importPtoFSinglePage(page:String, ws:WSClient, cc:ControllerComponents, actorRef: ActorRef)
  case class importCommitteesSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, sender:ActorRef)
  case class importPtoPSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, sender:ActorRef)
}

class ImportSinglePageActor extends Actor {

//  override def preStart(): Unit = Logger.info("supervised actor started " + self.path)
//  override def postStop(): Unit = Logger.info("supervised actor stopped " + self.path)
  implicit val timeout = Timeout(6 seconds)
  override def receive: Receive = {
    case importKmSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, sender:ActorRef) => {
      implicit val ec = cc.executionContext
      ws.url(page)
        .withFollowRedirects(true)
        .get()
        .map(res => {
          val xml = scala.xml.XML.loadString(res.body)
          val links = xml \ "link"
          if(getNext(links).isDefined){
            sender ! loadKmNextPage(getNext(links).get)
          }
          val currentKms = xml \ "entry" \ "content" \ "properties"
          val mappedKms = currentKms.map( node => {
            val name = (node \ "FirstName" text) + " " + (node \ "LastName" text)
            val gender = if( (node \ "GenderID").text == "251") "Male" else "Female"
            val knessetKey = (node \ "PersonID").text.toLong
            KnessetMember(-1L, name, gender, true, "", -1L, knessetKey)
          }).toSet
          val mappedEmails = currentKms.map( node => {
            ((node \ "PersonID").text, (node \ "Email").text)
          }).toMap
          sender ! newKms(mappedKms, mappedEmails)
          sender ! kmIspaFinish(self)
        })
    }
    case importPartiesSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, sender:ActorRef) => {
      implicit val ec = cc.executionContext
      ws.url(page)
        .withFollowRedirects(true)
        .get()
        .map(res => {
          val xml = scala.xml.XML.loadString(res.body)
          val links = xml \ "link"
          if(getNext(links).isDefined){
            sender ! loadPartyNextPage(getNext(links).get)
          }
          val relevantParties = (xml \ "entry" \ "content" \ "properties").filter( node => ( node \ "KnessetNum" text ) == knessetNum.toString )
          sender ! newParties(relevantParties.map(node => ((node \ "FactionID").text, (node \ "Name").text)).toMap)
          sender ! partiesIspaFinish(self)
        })
    }
    case importPtoFSinglePage(page:String, ws:WSClient, cc:ControllerComponents, sender:ActorRef) => {
      implicit val ec = cc.executionContext
      ws.url(page)
        .withFollowRedirects(true)
        .get()
        .map(res => {
          val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
          val links = scala.xml.XML.loadString(res.body) \ "link"
          if(getNext(links).isDefined){
            sender ! loadPtFNextPage(getNext(links).get)
          }
          sender ! newPersonToFaction(properties.map(node => (node \ "PersonID").text -> (node \ "FactionName").text).toMap)
          sender ! ptfIspaFinish(self)
        })
    }

    case importCommitteesSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, sender:ActorRef) => {
      implicit val ec = cc.executionContext
      ws.url(page)
        .withFollowRedirects(true)
        .get()
        .map(res => {
          val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
          val links = scala.xml.XML.loadString(res.body) \ "link"
          if(getNext(links).isDefined){
            sender ! loadCommNextPage(getNext(links).get)
          }
          val relevantCommittees = properties.filter( node => ( node \ "KnessetNum" text ) == knessetNum.toString )
          sender ! newCommittees(relevantCommittees.map( node => KmGroups(-1L, (node \ "Name").text, (node \ "CommitteeID").text.toLong, Set())).toSet)
          sender ! commIspaFinish(self)
        })
    }
    case importPtoPSinglePage(page:String, knessetNum:Int, ws:WSClient, cc:ControllerComponents, sender:ActorRef) => {
      implicit val ec = cc.executionContext
      ws.url(page)
        .withFollowRedirects(true)
        .get()
        .map(res => {
          val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
          val links = scala.xml.XML.loadString(res.body) \ "link"
          //TODO add the handling
          if(getNext(links).isDefined){
            sender ! loadPtoPNextPage(getNext(links).get)
          }
          val relevantPositions = properties.filter( node => ( node \ "KnessetNum" text ) == knessetNum.toString )
          sender ! newPtoP( relevantPositions.map( node => ( (node \ "CommitteeID").text, (node \ "PersonID").text) ).toSet )
          sender ! ptpIspaFinish(self)
        })
    }
  }

  private def getNext(nodes:NodeSeq): Option[String] = {
    val next = nodes.filter(node => (node \\ "@rel").text == "next").map(node => node \\ "@href")
    if(next.isEmpty) None
    else Some(next(0).text)
  }
}

object ImportCommitteesActor {
  def props = Props[ImportCommitteesActor]
  case class importCommittees(commFirstPage:String, ptpFirstPage:String, numPage:String)
  case class loadCommNextPage(nextPage:String)
  case class loadPtoPNextPage(nextPage:String)
  case class commIspaFinish(finished:ActorRef)
  case class ptpIspaFinish(finished:ActorRef)
  case class newCommittees(newCommittees:Set[KmGroups])
  case class newPtoP(newPtoP:Set[(String, String)])
}

class ImportCommitteesActor @Inject()(ws:WSClient, cc:ControllerComponents, knessetMembers: KnessetMemberDAO,
                                      kmGroups: KmGroupDAO, langs:Langs, messagesApi:MessagesApi ) extends Actor {
  private var knessetNum = 0
  private var commCount = 0
  private var ptpCount = 0
  private var committees:scala.collection.mutable.Set[KmGroups] = scala.collection.mutable.Set()
  private var ptp:scala.collection.mutable.Set[(String, String)] = scala.collection.mutable.Set() //(CommitteeID, PersonID)

  override def receive: Receive = {
    case importCommittees(commFirstPage:String, ptpFirstPage:String, numPage:String) => {
      implicit val ec = cc.executionContext
      ws.url(numPage)
        .withFollowRedirects(true)
        .get()
        .map(res => {
          val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
          val currentK = properties.filter(node => (node \ "IsCurrent" text).toBoolean).head
          knessetNum = (currentK \ "KnessetNum" text).toInt
          commCount += 1
          ptpCount  += 1
          var child = context.actorOf(ImportSinglePageActor.props)
          child ! importCommitteesSinglePage(commFirstPage, knessetNum, ws, cc, self)
          child = context.actorOf(ImportSinglePageActor.props)
          child ! importPtoPSinglePage(ptpFirstPage, knessetNum, ws, cc, self)
        })
    }
    case loadCommNextPage(nextPage:String) => {
      commCount += 1
      val child = context.actorOf(ImportSinglePageActor.props)
      child ! importCommitteesSinglePage(nextPage, knessetNum, ws, cc, self)
    }
    case loadPtoPNextPage(nextPage:String) => {
      ptpCount += 1
      val child = context.actorOf(ImportSinglePageActor.props)
      child ! importPtoPSinglePage(nextPage, knessetNum, ws, cc, self)
    }
    case newCommittees(newCommittees:Set[KmGroups]) => {
        committees ++= newCommittees
    }
    case newPtoP(newPtoP:Set[(String, String)]) => {
        ptp ++= newPtoP
    }
    case commIspaFinish(finished:ActorRef) => {
      context.stop(finished)
      commCount -= 1
      if( commCount == 0 & ptpCount == 0 ){
        updateAll()
      }
    }
    case ptpIspaFinish(finished:ActorRef) => {
      context.stop(finished)
      ptpCount -= 1
      if( commCount == 0 & ptpCount == 0 ){
        updateAll()
      }
    }
  }

  private def updateAll() = {
    implicit val ec = cc.executionContext
    for {
      activeKms <- knessetMembers.getAllActiveKms()
      knessetKeyToKmID = activeKms.map( km => (km.knessetKey, km.id) ).toMap
    } yield {
      ptp = ptp.filter(p => committees.map(_.knessetKey).contains(p._1.toLong))
      val members = (p:String, links:mutable.Set[(String,String)]) => links.filter(_._1 == p).map(_._2)
      val comWithPersonsImmutable = ptp.map( p => ( p._1, members(p._1, ptp))).toMap
      var comWithPersons:scala.collection.mutable.Map[String, mutable.Set[String]] = collection.mutable.Map(comWithPersonsImmutable.toSeq: _*)
      committees.foreach( com => if( !comWithPersons.contains(com.knessetKey.toString)) comWithPersons += (com.knessetKey.toString -> mutable.Set()) )
      val updatedGroups = committees.map(group => {
        val setOfKms = comWithPersons(group.knessetKey.toString).map(p => knessetKeyToKmID(p.toLong) )
        KmGroups(group.id, group.name, group.knessetKey, setOfKms.toSet)
      })
      kmGroups.updateGroups(updatedGroups.toSeq)
    }
  }
}