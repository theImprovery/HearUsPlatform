package actors

import actors.ImportSinglePageActor.{ImportSingleCommitteePage, ImportSinglePersonToPositionPage}
import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import dataaccess.{KmGroupDAO, KnessetMemberDAO}
import javax.inject.{Inject, Named}
import models.KmGroup
import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents

import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}

object ImportCommitteesActor {
  def props = Props[ImportCommitteesActor]
  case class ImportCommittees(commFirstPage:String, ptpFirstPage:String, numPage:String)
  case class LoadCommitteePage(nextPage:String)
  case class LoadPerson2PositionPPage(nextPage:String)
  case class AddCommittees(newCommittees:Set[KmGroup])
  case class AddPersonToPosition(newPtoP:Set[PersonToPosition])
}

class ImportCommitteesActor @Inject()(ws:WSClient, knessetMembers: KnessetMemberDAO,
                                      ec:ExecutionContext, kmGroups: KmGroupDAO )
  extends BaseKnessetActor(ec, ws) {
  import ImportCommitteesActor._
  val logger = Logger(classOf[ImportCommitteesActor])

  private var knessetNum = 0
  private var activeCommitteePageImports = 0
  private var activeP2pImports = 0
  private val committees:scala.collection.mutable.Set[KmGroup] = scala.collection.mutable.Set()
  private val ptp:scala.collection.mutable.Set[PersonToPosition] = scala.collection.mutable.Set()
  
  override def receive: Receive = {
    case ImportCommittees(commFirstPage:String, ptpFirstPage:String, knessetNumEndpoint:String) => {
      logger.info("Importing committees")
      get(knessetNumEndpoint, res => {
        logger.info("Initial WS call returned")
        val properties = scala.xml.XML.loadString(res.body) \ "entry" \ "content" \ "properties"
        val currentK = properties.filter(node => (node \ "IsCurrent").text.toBoolean).head
        knessetNum = (currentK \ "KnessetNum").text.toInt
        logger.info(s"Current knesset number: $knessetNum")
        
        self ! LoadCommitteePage(s"$commFirstPage and KnessetNum eq $knessetNum")
        self ! LoadPerson2PositionPPage(s"$ptpFirstPage and KnessetNum eq $knessetNum")
        
        logger.info("Committee import initiated")
      })
    }
    
    case LoadCommitteePage(nextPage) => {
      activeCommitteePageImports += 1
      logger.info("requested to load next committee page")
      children.route(ImportSingleCommitteePage(nextPage, knessetNum, self), self)
    }
    
    case LoadPerson2PositionPPage(nextPage) => {
      activeP2pImports += 1
      children.route(ImportSinglePersonToPositionPage(nextPage, knessetNum, self), self)
    }
    
    case AddCommittees(newCommittees) => {
      committees ++= newCommittees
      activeCommitteePageImports -= 1
      possiblyFinish()
    }
    
    case AddPersonToPosition(newPtoP) => {
      ptp ++= newPtoP
      activeP2pImports -= 1
      possiblyFinish()
    }
  }

  private def possiblyFinish():Unit = {
    logger.info(s"import status: activeCommitteePageImports=$activeCommitteePageImports, activeP2pImports=$activeP2pImports")
    if ( activeCommitteePageImports == 0 & activeP2pImports == 0 ) {
      updateAll()
    }
  }
  
  private def updateAll():Unit = {
    implicit val ecc:ExecutionContext = ec
    logger.info("Committee import data collection done. Updating DB.")
    for {
      activeKms <- knessetMembers.getAllActiveKms()
      knessetKey2KmId = activeKms.map( km => km.knessetKey->km.id ).toMap
    } yield {
      logger.info( s"Found ${committees.size} committees and ${ptp.size} memberships")
      logger.info( s"Committees: \n " + committees.toSeq.sortBy(_.knessetKey).map( c => s"${c.knessetKey}: \t ${c.name}").mkString("\n") )
      
      val newCommitteeKK = committees.map( _.knessetKey ).toSet
      val activePtPs = ptp.filter( p => newCommitteeKK(p.committeeKnessetKey) && knessetKey2KmId.contains(p.personId) ).toSet
      logger.info(s"Active PtPs: ${activePtPs.size}")
      
      // group by committee, using Knesset keys.
      val cmtKey2PsnKey = activePtPs.groupMap( _.committeeKnessetKey )(_.personId)
      // convert MKs Knesset keys to db ids.
      val cmtKey2PsnId = cmtKey2PsnKey.map( t => (t._1, t._2.map(knessetKey2KmId).toSet))
      // add committee members
      val committeesWithMembers = committees.map( cmt => cmt.copy(kms=cmtKey2PsnId.getOrElse(cmt.knessetKey, Set())))
      
      kmGroups.updateGroups(committeesWithMembers.toSeq)
      logger.info("Committee import done.")
    }
  }
}
