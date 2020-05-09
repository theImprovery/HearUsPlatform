package controllers

import actors.ImportCommitteesActor.importCommittees
import actors.ImportCoordinationActor.{getKnessetNum, importAll}
import akka.actor.ActorRef
import be.objectify.deadbolt.scala.{DeadboltActions, allOfGroup}
import dataaccess.{ImagesDAO, KmGroupDAO, KnessetMemberDAO}
import javax.inject.{Inject, Named}

import play.api.{Configuration, Logger}
import play.api.i18n._
import play.api.libs.ws.WSClient
import akka.pattern.ask
import akka.util.Timeout
import models.UserRole
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

  def apiKms = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    implicit val timeout = Timeout(60.seconds)
    importActor ? importAll(
      conf.get[String]("xml.km"),
      conf.get[String]("xml.factions"),
      conf.get[String]("xml.knessetDates"),
      conf.get[String]("xml.ptpParties"))
    Future(Ok("updated"))
  }

  def apiUpdateCommittees = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    implicit val timeout = Timeout(60.seconds)
    committeeActor ? importCommittees(
      conf.get[String]("xml.committees"),
      conf.get[String]("xml.ptpCommittees"),
      conf.get[String]("xml.knessetDates"))
    Future(Ok("update"))
  }


}