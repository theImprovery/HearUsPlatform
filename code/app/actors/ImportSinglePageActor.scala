package actors

import actors.ImportCommitteesActor._
import actors.ImportCoordinationActor._
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import javax.inject.Inject
import models.{KmGroup, KnessetMember}
import play.api.Logger
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.ControllerComponents

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

object ImportSinglePageActor {
  def nameOf( i:Int ) = s"${classOf[ImportSinglePageActor].getCanonicalName}-$i"
  val count = 3
  
  case class ImportSingleKmPage(page:String, knessetNum:Int, mgr:ActorRef)
  case class ImportSinglePartyPage(page:String, knessetNum:Int, mgr:ActorRef)
  case class ImportSinglePerson2FactionPage(page:String, mgr:ActorRef)
  case class ImportSingleCommitteePage(page:String, knessetNum:Int, mgr:ActorRef)
  case class ImportSinglePersonToPositionPage(page:String, knessetNum:Int, mgr:ActorRef)
}

/**
  * Child actor, used to import a single page of entities, and return the result to its parent actor.
  * @param cc
  * @param ws
  */
class ImportSinglePageActor @Inject() (anEx:ExecutionContext, cc:ControllerComponents, ws:WSClient) extends Actor {
  import ImportSinglePageActor._
  implicit val timeout:Timeout = Timeout(6.seconds)
  implicit val ec:ExecutionContext = anEx
  private val logger = Logger(classOf[ImportSinglePageActor])
  
  override def preStart(): Unit = {
    logger.info("ImportSinglePageActor started" )
  }
  
  override def receive: Receive = {
    case ImportSingleKmPage(page:String, knessetNum:Int, mgr:ActorRef) => {
      get(page, res => {
          val xml = scala.xml.XML.loadString(res.body)
          val links = xml \ "link"
          getNext(links).foreach( next => mgr ! LoadKmPage(next))
          
          val kmNodes = xml \ "entry" \ "content" \ "properties"
          val kms     = kmNodes.filter( n => (n\"IsCurrent").text.trim.toLowerCase == "true")
            .map( node => {
              val name = (node \ "FirstName").text + " " + (node \ "LastName").text
              val gender = if( (node \ "GenderID").text == "251") "Male" else "Female"
              val knessetKey = (node \ "PersonID").text.toLong
              KnessetMember(-1L, name, gender, isActive = true, "", -1L, knessetKey)
          }).toSet
          val mappedEmails = kmNodes.map( node => {
            ((node \ "PersonID").text, (node \ "Email").text)
          }).toMap
          mgr ! AddKms(kms, mappedEmails)
      })
    }
    
    case ImportSinglePartyPage(page:String, knessetNum:Int, mgr:ActorRef) => {
      get(page, res => {
        val xml = scala.xml.XML.loadString(res.body)
        val links = xml \ "link"
        
        getNext(links).foreach( next => mgr ! LoadPartyPage(next) )
        
        val relevantParties = (xml \ "entry" \ "content" \ "properties").filter( node => (node\"KnessetNum").text == knessetNum.toString )
        mgr ! AddParties(relevantParties.map(node => ((node \ "FactionID").text, (node \ "Name").text)).toMap)
      })
    }
    
    case ImportSinglePerson2FactionPage(page:String, mgr:ActorRef) => {
      get(page, res => {
          val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
          val links = scala.xml.XML.loadString(res.body) \ "link"
          getNext(links).foreach(next => mgr ! LoadPsn2FcnPage(next))
          
          mgr ! AddPsn2Fcn(properties.map(node => (node \ "PersonID").text -> (node \ "FactionName").text).toMap)
        })
    }

    case ImportSingleCommitteePage(page:String, knessetNum:Int, mgr:ActorRef) => {
      logger.info(s"importing single committee page")
      get(page, res => {
          val xml = scala.xml.XML.loadString(res.body)
          val properties = xml \ "entry" \ "content" \ "properties"
          val links = xml \ "link"
          getNext(links).foreach( nextPageLink => {
            mgr ! LoadCommitteePage(nextPageLink)
          })
          
          val relevantCommittees = properties.filter( node => (node\"KnessetNum").text == knessetNum.toString )
          mgr ! AddCommittees(relevantCommittees.map(node => KmGroup(-1L, (node \ "Name").text, (node \ "CommitteeID").text.toLong, Set())).toSet)
        })
    }
    
    case ImportSinglePersonToPositionPage(page:String, knessetNum:Int, mgr:ActorRef) => {
      logger.info(s"Importing p2p: $page")
      get(page, res => {
        val xml = scala.xml.XML.loadString(res.body)
        val properties = xml \ "entry" \ "content" \ "properties"
        logger.info( s"properties #: ${properties.length}" )
        
        val links = xml \ "link"
        getNext(links).foreach( nextPageLink => {
          mgr ! LoadPerson2PositionPPage(nextPageLink)
        })
        val relevantPositions = properties.filter( node => (node\"IsCurrent").text=="true" && (node\"CommitteeID").text.trim.nonEmpty )
        val p2ps = relevantPositions.map(node => ( (node\"PersonID").text, (node\"CommitteeID").text) )
                    .map( t => PersonToPosition(t._1.toLong, t._2.toLong) ).toSet
        mgr ! AddPersonToPosition( p2ps )
      })
    }
  }
  
  private def get(page:String, handler:WSResponse=>Unit ):Unit =  {
    ws.url(page).withFollowRedirects(true).get().map( handler )(cc.executionContext)
  }
  
  private def getNext(nodes:NodeSeq): Option[String] = {
    nodes.filter(node => (node \\ "@rel").text == "next")
      .map(node => node \\ "@href")
      .headOption
      .map( _.text )
  }
}
