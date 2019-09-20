package controllers

import java.nio.file.{CopyOption, Files, Path, Paths, StandardCopyOption}

import be.objectify.deadbolt.scala.{DeadboltActions, allOfGroup}
import javax.inject.Inject
import models._
import dataaccess._
import play.api.{Configuration, Environment, Logger}
import play.api.data.{Form, _}
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{ControllerComponents, InjectedController}
import dataaccess.JSONFormats._
import play.api.libs.ws.WSClient

import scala.collection.JavaConversions._
import scala.concurrent.Future

case class GroupData(id:Long, name:String, knessetKey:Long, kmsIds:String)

class KnessetMemberCtrl @Inject()(deadbolt:DeadboltActions, cc:ControllerComponents, kms:KnessetMemberDAO,
                                  images: ImagesDAO, groups: KmGroupDAO, langs:Langs, messagesApi:MessagesApi,
                                  conf:Configuration, ws:WSClient) extends InjectedController {

  implicit private val ec = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  val knessetMemberForm = Form(
    mapping(
      "id" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "name" -> nonEmptyText,
      "gender" -> text,
      "isActive" -> boolean,
      "webPage" -> text,
      "partyId" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "knessetKey" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int])
    )(KnessetMember.apply)(KnessetMember.unapply)
  )

  val groupForm = Form(
    mapping(
      "id" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "name" -> nonEmptyText,
      "knessetKey" -> number.transform[Long](_.asInstanceOf[Long], _.asInstanceOf[Int]),
      "kmsIds" -> text
    )(GroupData.apply)(GroupData.unapply)
  )

  def showParties = deadbolt.SubjectPresent()() { implicit req =>
    for {
      parties <- kms.getAllParties
    } yield {
      Ok(views.html.knesset.parties(parties.sortBy(_.name)))
    }
  }

  def showKms(search: Option[String], asc: Option[String], sortByOpt: Option[String]) = deadbolt.SubjectPresent()() { implicit req =>
    val effectiveSearch = search.map(_.trim).flatMap(v => if (v.isEmpty) None else Some(v))
    val sortBy: SortBy.Value = sortByOpt.flatMap(v => SortBy.values.find(_.toString == v)).getOrElse(SortBy.KnessetMember)
    val sqlSearch = search.map(s => "%" + s.trim + "%")
    val isAsc = asc.getOrElse("1") == "1"
    for {
      knessetMembers <- kms.getKms(sqlSearch, isAsc, sortBy)
      parties <- kms.getAllParties
    } yield {
      val partyMap = parties.map(p => p.id -> p).toMap
      Ok(views.html.knesset.knessetMembers(knessetMembers, effectiveSearch, isAsc, sortBy))
    }
  }

  def showNewKM() = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))() { implicit req =>
    for {
      parties <- kms.getAllParties
    } yield {
      Ok(views.html.knesset.knessetMemberEditor(knessetMemberForm, conf.get[String]("hearUs.files.mkImages.url"), None,
        parties.map(p => (p.id, p.name)).toMap, Platform.values.toSeq))
    }
  }

  def showEditKM(id: Long) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    for {
      km <- kms.getKM(id)
      parties <- kms.getAllParties
      imageOpt <- images.getImageForKm(id)
    } yield {
      km.map(m => Ok(views.html.knesset.knessetMemberEditor(knessetMemberForm.fill(m),
        conf.get[String]("hearUs.files.mkImages.url"), imageOpt,
        parties.map(p => (p.id, p.name)).toMap, Platform.values.toSeq)))
        .getOrElse(NotFound("Knesset member with id " + id + "does not exist"))
    }
  }

  def doEditKM() = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    knessetMemberForm.bindFromRequest().fold(
      formWithErrors => {
        for {
          knessetMembers <- kms.getAllKms
          parties <- kms.getAllParties
          imageOpt <- images.getImageForKm(formWithErrors.data("id").toLong)
        } yield {
          Logger.info(formWithErrors.errors.mkString("\n"))
          val partyMap = parties.map(p => p.id -> p).toMap
          BadRequest(views.html.knesset.knessetMemberEditor(formWithErrors,
            conf.get[String]("hearUs.files.mkImages.url"), imageOpt,
            parties.map(p => (p.id, p.name)).toMap, Platform.values.toSeq))
        }
      },
      knessetMember => {
        val message = Informational(InformationalLevel.Success, Messages("knessetMember.update"))
        kms.addKM(knessetMember).map(newKm => Redirect(routes.KnessetMemberCtrl.showEditKM(newKm.id)).flashing(FlashKeys.MESSAGE -> message.encoded))
      }
    )
  }

  def deleteKM(id: Long) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    for {
      deleted <- kms.deleteKM(id)
      knessetMembers <- kms.getKms(None, true, SortBy.KnessetMember)
      parties <- kms.getAllParties
    } yield {
      val partyMap = parties.map(p => p.id -> p).toMap
      Ok(views.html.knesset.knessetMembers(knessetMembers, None, true, SortBy.KnessetMember)).flashing(FlashKeys.MESSAGE -> messagesProvider.messages("knessetMember.deleted"))
    }
  }

  def getContactOptionForKm(id: Long) = deadbolt.SubjectPresent()() { implicit req =>
    kms.getContactOptions(id).map(cos => Ok(Json.toJson(cos)))
  }

  def updateContactOption(kmId: Long) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(cc.parsers.tolerantJson){ implicit req =>
    req.body.validate[Seq[ContactOption]].fold(
      errors => {
        Logger.warn(errors.mkString("\n"))
        Future(BadRequest(Json.obj("status" -> "error", "data" -> JsError.toJson(errors))))
      },
      cos => {
        kms.setContactOptions(kmId, cos).map(_ => Ok(Json.toJson("message"->"ok")) )
      }
    )
  }

  def updateParty() = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(cc.parsers.tolerantJson){ implicit req =>
    req.body.validate[Party].fold(
      errors => {
        for {
          parties <- kms.getAllParties
        } yield {
          Logger.info(errors.mkString("\n"))
          BadRequest(views.html.knesset.parties(parties))
        }
      },
      party => {
        kms.addParty(party).map(newP => Ok(Json.toJson(newP)))
      }
    )
  }

  def deleteParty(id: Long) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    for {
      deleted <- kms.deleteParty(id)
      parties <- kms.getAllParties
    } yield {
      Ok(views.html.knesset.parties(parties))
    }
  }

  def getImage(imageName: String) = Action.async { implicit req =>
    val kmId = imageName.split("\\.")(0)

    images.getImageForKm(kmId.toInt).map({
      case Some(img) => {
        val path = Paths.get(conf.get[String]("hearUs.files.mkImages.folder")).resolve(imageName)
        Ok(Files.readAllBytes(path)).as(img.mimeType)
      }
      case None => NotFound("Can't find image.")
    })
  }

  def showGroups = deadbolt.SubjectPresent()() { implicit req =>
    for {
      groupList <- groups.allGroupsDN(None).map( _.sortBy(_.name))
    } yield {
      Ok(views.html.knesset.groups(groupList))
    }
  }

  def showNewGroup = deadbolt.SubjectPresent()() { implicit req =>
    for {
      knessetMembers <- kms.getAllKms
    } yield {
      Ok(views.html.knesset.groupEditor(groupForm, knessetMembers))
    }
  }

  def showEditGroup(id: Long) =  deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    for {
      groupOpt <- groups.getGroupDN(id)
      groupKms <- groups.getKmForGroup(id)
      knessetMembers <- kms.getAllKms
    } yield {
      groupOpt match {
        case Some(g) => Ok(views.html.knesset.groupEditor(groupForm.fill(GroupData(g.id, g.name, g.knessetKey, groupKms.mkString(","))),
          knessetMembers.sortBy(_.name)))
        case None => NotFound("Group with id " + id + "does not exist")
      }
    }
  }

  def doEditGroup = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    groupForm.bindFromRequest().fold(
      formWithErrors => {
        for {
          knessetMembers <- kms.getAllKms
        } yield {
          Logger.info(formWithErrors.errors.mkString("\n"))
          BadRequest(views.html.knesset.groupEditor(formWithErrors, knessetMembers))
        }
      },
      data => {
        val insGroup = if (data.kmsIds == "") KmGroup(data.id, data.name, data.knessetKey, Set[Long]()) else {
          KmGroup(data.id, data.name, data.knessetKey, data.kmsIds.split(",", -1).map(id => id.toLong).toSet)
        }
        groups.addGroup(insGroup).map { group =>
          val message = Informational(InformationalLevel.Success, Messages("groupEditor.savedSuccessfully", group.name))
          Redirect(routes.KnessetMemberCtrl.showGroups()).flashing(FlashKeys.MESSAGE -> message.encoded)
        }
      }
    )
  }

  def deleteGroup(id: Long) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    for {
      deleted <- groups.deleteGroup(id)
      groups <- groups.allGroupsDN(None)
    } yield {
      Ok(views.html.knesset.groups(groups))
    }
  }
  
}