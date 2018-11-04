package controllers

import java.nio.file.{Files, Paths}

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions}
import javax.inject.Inject
import models._
import dataaccess.{ImagesDAO, KmGroupDAO, KnessetMemberDAO, Platform}
import play.api.{Configuration, Logger}
import play.api.data.{Form, _}
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, ControllerComponents, InjectedController}
import dataaccess.JSONFormats._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class GroupData(id:Long, name:String, kmsIds:String)

class KnessetMemberCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                  images: ImagesDAO, groups: KmGroupDAO,
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

  val groupForm = Form(
    mapping(
      "id"->number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "name"->nonEmptyText,
      "kmsIds"->text
    )(GroupData.apply)(GroupData.unapply)
  )

  def showParties = deadbolt.SubjectPresent()() { implicit req =>
    for {
      parties <- kms.getAllParties
    } yield {
      Ok( views.html.knesset.parties(parties.sortBy(_.name)) )
    }
  }

  def showKms = deadbolt.SubjectPresent()() { implicit req =>
    for {
      knessetMembers <- kms.getAllKms
      parties <- kms.getAllParties
    } yield {
      val partyMap = parties.map(p=>p.id->p).toMap
      Ok( views.html.knesset.knessetMembers(knessetMembers.sortBy(_.name), partyMap) )
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
          parties <- kms.getAllParties
        } yield {
          Logger.info( formWithErrors.errors.mkString("\n") )
          val partyMap = parties.map(p=>p.id->p).toMap
          BadRequest( views.html.knesset.knessetMembers(knessetMembers, partyMap) )
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
      deleted <- kms.deleteKM(id)
      knessetMembers <- kms.getAllKms
      parties <- kms.getAllParties
    } yield {
      val partyMap = parties.map(p=>p.id->p).toMap
      Ok(views.html.knesset.knessetMembers(knessetMembers, partyMap)).flashing(FlashKeys.MESSAGE->messagesProvider.messages("knessetMember.deleted"))
    }
  }

  def getContactOptionForKm(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    kms.getContactOptions(id).map( cos => Ok( Json.toJson(cos)) )
  }

  def updateContactOption(id:Long) = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Seq[ContactOption]].fold(
      errors => Future(BadRequest(Json.obj("status" -> "error", "data" -> JsError.toJson(errors)))),
      cos => {
        kms.addContactOption(cos).map(ans => Ok(Json.toJson(ans)))
      }
    )
  }

  def updateParty() = deadbolt.SubjectPresent()(cc.parsers.tolerantJson) { implicit req =>
    req.body.validate[Party].fold(
      errors => {
        for {
          parties <- kms.getAllParties
        } yield {
          Logger.info( errors.mkString("\n") )
          BadRequest( views.html.knesset.parties(parties))
        }
      },
      party => {
        kms.addParty(party).map(newP => Ok(Json.toJson(newP)))
      }
    )
//    partyForm.bindFromRequest().fold(
//      errors => {
//        for {
//          parties <- kms.getAllParties
//        } yield {
//          Logger.info( errors.errors.mkString("\n") )
//          BadRequest( views.html.knesset.parties(parties))
//        }
//      },
//      party => {
//        kms.addParty(party).map(newP => Redirect(routes.KnessetMemberCtrl.showParties()))
//      }
//    )
  }

  def deleteParty(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for{
      deleted <- kms.deleteParty(id)
      parties <- kms.getAllParties
    } yield {
      Ok(views.html.knesset.parties(parties))
    }
  }
  
  def getImage(id:Long) = Action.async { implicit req =>
    images.getFileForKM(id).map {
      case None => NotFound("image not found")
      case Some(image) => {
        val path = Paths.get(conf.get[String]("hear_us.files.km.folder"))
                        .resolve(image.kmId.toString)
                        .resolve(image.id.toString + "." + image.suffix)
        Ok(Files.readAllBytes(path)).as(image.mimeType)
      }
    }
  }

  def showGroups = deadbolt.SubjectPresent()() { implicit req =>
    for {
      groupList <- groups.allGroupsDN
    } yield {
      Ok( views.html.knesset.groups(groupList))
    }
  }

  def showNewGroup = deadbolt.SubjectPresent()() { implicit req =>
    for {
      knessetMembers <- kms.getAllKms
    } yield {
      Ok( views.html.knesset.groupEditor(groupForm, knessetMembers))
    }
  }

  def showEditGroup(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for{
      group <- groups.getGroupDN(id)
      groupKms <- groups.getKmForGroup(id)
      knessetMembers <- kms.getAllKms
    } yield {
      group.map( g => Ok( views.html.knesset.groupEditor(groupForm.fill(GroupData(g.id, g.name, groupKms.mkString(","))),
        knessetMembers))).getOrElse( NotFound("Group with id " + id + "does not exist") )
    }
  }

  def doEditGroup = deadbolt.SubjectPresent()() { implicit req =>
    groupForm.bindFromRequest().fold(
      formWithErrors => {
        for {
          groupList <- groups.allGroupsDN
        } yield {
          Logger.info( formWithErrors.errors.mkString("\n") )
          BadRequest( views.html.knesset.groups(groupList))
        }
      },
      data => {
        val insGroup = if(data.kmsIds=="") KmGroups(data.id, data.name, Set[Long]()) else {
          KmGroups(data.id, data.name, data.kmsIds.split(",",-1).map(id => id.toLong).toSet)
        }
        groups.addGroup(insGroup).map( group => Redirect(routes.KnessetMemberCtrl.showEditGroup(group.id)))
      }
    )
  }

  def deleteGroup(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    for{
      deleted <- groups.deleteGroup(id)
      groups <- groups.allGroupsDN
    } yield {
      Ok(views.html.knesset.groups(groups))
    }
  }
}
