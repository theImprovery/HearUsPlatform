package controllers

import actors.ImportCommitteesActor.importCommittees
import actors.ImportCoordinationActor.{getKnessetNum, importAll}
import akka.actor.ActorRef
import be.objectify.deadbolt.scala.DeadboltActions
import dataaccess.{ImagesDAO, KmGroupDAO, KnessetMemberDAO}
import javax.inject.{Inject, Named}
import play.api.Configuration
import play.api.i18n._
import play.api.libs.ws.WSClient
import akka.pattern.ask
import akka.util.Timeout
import play.api.mvc.{ControllerComponents, InjectedController}

import scala.concurrent.duration._
import scala.concurrent.Future

class ParseCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                  messagesApi:MessagesApi, langs:Langs, conf:Configuration,
                                  @Named("import-actor") importActor:ActorRef, @Named("committee-actor") committeeActor:ActorRef) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  def apiKms = deadbolt.SubjectPresent()() { implicit req =>
    implicit val timeout = Timeout(6 seconds)
    importActor ? importAll("http://localhost:8080/projects/HearUsPlatform/code/public/xmls/km.xml",
      "http://localhost:8080/projects/HearUsPlatform/code/public/xmls/factions.xml",
      "http://localhost:8080/projects/HearUsPlatform/code/public/xmls/knessetDates.xml",
      "http://localhost:8080/projects/HearUsPlatform/code/public/xmls/ptp.xml")
    Future(Ok("updated"))
  }

  def apiUpdateCommittees = deadbolt.SubjectPresent()() { implicit req =>
    implicit val timeout = Timeout(6 seconds)
    committeeActor ? importCommittees("http://localhost:8080/projects/HearUsPlatform/code/public/xmls/committee.xml",
      "http://localhost:8080/projects/HearUsPlatform/code/public/xmls/ptpCom.xml",
      "http://localhost:8080/projects/HearUsPlatform/code/public/xmls/knessetDates.xml")
    Future(Ok("update"))
  }


}