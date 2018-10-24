package controllers

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions}
import javax.inject.Inject
import models.{ContactOption, KMImage, KnessetMember, Party}
import dataaccess.{KnessetMemberDAO, Platform}
import play.api.{Configuration, Logger}
import play.api.data.{Form, _}
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, ControllerComponents, InjectedController}
import dataaccess.JSONFormats._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class KnessetMemberCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                  langs:Langs, messagesApi:MessagesApi, conf:Configuration) extends InjectedController{

  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  val knessetMemberForm = Form(
    mapping(
      "id"->number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "name"->nonEmptyText,
      "gender"->text,
      "isActive"->boolean,
      "webPage"->text,
      "partyId"->number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int])
    )(KnessetMember.apply)(KnessetMember.unapply)
  )

  val partyForm = Form(
    mapping(
      "id"->number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "name"->text,
      "webPage"->text
    )(Party.apply)(Party.unapply)
  )



  def showParties = deadbolt.SubjectPresent()() { implicit req =>
    for {
      parties <- kms.getAllParties
    } yield {
      Ok( views.html.knesset.parties(parties) )
    }
  }

  def showKms = deadbolt.SubjectPresent()() { implicit req =>
    for {
      knessetMembers <- kms.getAllKms
    } yield {
      Ok( views.html.knesset.knessetMembers(knessetMembers) )
    }
  }

  def showNewKM() = deadbolt.SubjectPresent()() { implicit req =>
    for {
      parties <- kms.getAllParties
    }yield {
      Ok( views.html.knesset.knessetMemberEditor(knessetMemberForm, conf.get[String]("hear_us.files.url"),
                                                 parties.map(p => (p.id, p.name)).toMap, Platform.values.toSeq))
    }
  }

  def showEditKM(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for{
      km <- kms.getKM(id)
      parties <- kms.getAllParties
    } yield {
      km.map( m => Ok( views.html.knesset.knessetMemberEditor(knessetMemberForm.fill(m), conf.get[String]("hear_us.files.url"),
                                                              parties.map(p => (p.id, p.name)).toMap, Platform.values.toSeq)) )
        .getOrElse( NotFound("Knesset member with id " + id + "does not exist") )
    }
  }

  def doEditKM() = deadbolt.SubjectPresent()() { implicit req =>
    knessetMemberForm.bindFromRequest().fold(
      formWithErrors => {
        for {
          knessetMembers <- kms.getAllKms
        } yield {
          Logger.info( formWithErrors.errors.mkString("\n") )
          BadRequest( views.html.knesset.knessetMembers(knessetMembers) )
        }
      },
      knessetMember => {
        val message = Informational(InformationalLevel.Success, Messages("knessetMember.update"))
        kms.addKM(knessetMember).map( newKm => Redirect(routes.KnessetMemberCtrl.showEditKM(newKm.id)).flashing(FlashKeys.MESSAGE->message.encoded))
      }
    )
  }

  def deleteKM(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for{
      knessetMembers <- kms.getAllKms
      deleted <- kms.deleteKM(id)
    } yield {
      Ok(views.html.knesset.knessetMembers(knessetMembers))
    }
  }

  def getContactOptionForKm(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    kms.getContactOptions(id).map( cos => Ok( Json.toJson(cos)) )
  }

  def updateContactOption(id:Long) = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    Logger.info("update")
    req.body.validate[Seq[ContactOption]].fold(
      errors => Future(BadRequest(Json.obj("status"->"error", "data"->JsError.toJson(errors)))),
      cos => {
        Logger.info("ans " + cos.mkString("\n"))
        Future(Ok("ok"))
      }
    )
  }

  def updateParty() = deadbolt.SubjectPresent()() { implicit req =>
    partyForm.bindFromRequest().fold(
      errors => {
        for {
          parties <- kms.getAllParties
        } yield {
          Logger.info( errors.errors.mkString("\n") )
          BadRequest( views.html.knesset.parties(parties))
        }
      },
      party => {
        kms.addParty(party).map(newP => Redirect(routes.KnessetMemberCtrl.showParties()))
      }
    )
  }

  def deleteParty(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for{
      parties <- kms.getAllParties
      deleted <- kms.deleteParty(id)
    } yield {
      Ok(views.html.knesset.parties(parties))
    }
  }
}
