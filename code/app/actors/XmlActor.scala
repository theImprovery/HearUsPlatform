package actors

import actors.XmlActor.GetKms
import akka.actor.{Actor, Props}
import javax.inject._
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Logger}

import scala.concurrent.{Await, Future}
import scala.xml.NodeSeq


object XmlActor {
  def props = Props[XmlActor]
  case class GetKms(firstPage:String)
}

class XmlActor @Inject()(config:Configuration, ws:WSClient, cc:ControllerComponents) extends Actor {
  implicit private val ec = cc.executionContext
  var ans = Seq[NodeSeq]()
  override def receive: Receive = {
    case GetKms(firstPage: String) => {
      getKmsForPage(firstPage)
      sender ! ans
    }
  }

  private def getKmsForPage(page:String):Future[Unit] = {
    ws.url(page)
      .withFollowRedirects(true)
      .get()
      .map(res => {
        ans ++= scala.xml.XML.loadString(res.body)
        val xml = scala.xml.XML.loadString(res.body)
        val links = xml \ "link"
        if(getNext(links).isDefined) getKmsForPage(getNext(links).get)
      })
  }

  private def getNext(nodes:NodeSeq): Option[String] = {
    val next = nodes.filter(node => (node \\ "@rel").text == "next").map(node => node \\ "@href")
    if(next.isEmpty) None
    else Some(next(0).text)
  }
}