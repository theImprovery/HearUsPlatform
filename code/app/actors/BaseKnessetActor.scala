package actors

import akka.actor.Actor
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import play.api.Logger
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class BaseKnessetActor(anEc:ExecutionContext, ws:WSClient) extends Actor {
  
  val logger:Logger
  implicit protected val ec:ExecutionContext = anEc
  implicit protected val to:Timeout =  Timeout(10.seconds)
  private var _children:Router = _
  
  protected def get(page:String, handler:WSResponse=>Unit ):Unit =  {
    ws.url(page).withFollowRedirects(true).get().map( handler )
  }
  
  protected def children:Router = {
    if ( _children == null ) lazyInitChildren()
    _children
  }
  
  private def lazyInitChildren(): Unit = {
    logger.info("looking for sips...")
    val futures = Range(0, ImportSinglePageActor.count).map( n => s"akka://application/user/${ImportSinglePageActor.nameOf(n)}" )
      .map( n => {logger.info(n); n} )
      .map( context.actorSelection )
      .map( _.resolveOne() )
    val seq = Await.result(Future.sequence(futures), 10.seconds )
    logger.info(s"found ${seq.length} sips")
    _children = Router( RoundRobinRoutingLogic(), seq.map(ActorRefRoutee) )
  }
  
}
