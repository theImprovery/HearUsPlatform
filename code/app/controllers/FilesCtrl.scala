package controllers

import java.nio.file.{Files, Paths}
import java.sql.Timestamp
import java.util.Calendar

import javax.inject.Inject
import be.objectify.deadbolt.scala.DeadboltActions
import dataaccess.ImagesDAO
import dataaccess.JSONFormats._
import models.KMImage
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{ControllerComponents, InjectedController, PlayBodyParsers}
import play.api.{Configuration, Logger}
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class FilesCtrl @Inject() (images:ImagesDAO, cc:ControllerComponents, parsers:PlayBodyParsers,
                           config:Configuration, deadbolt:DeadboltActions) extends InjectedController {

  implicit private val ec: ExecutionContext = cc.executionContext

  def apiAddFile(subjectId: String) = deadbolt.SubjectPresent()(cc.parsers.multipartFormData ){ req =>
    val uploadedFile = req.body.files.head
    val filePath = Paths.get(config.get[String]("hear_us.files.km.folder")).resolve(subjectId)
    Files.createDirectories( filePath )
    val filename = req.body.dataParts("qqfilename").head
    val suffix = getFileSuffix(filename)
    images.addFile( KMImage(0L, subjectId.toLong,  suffix, "", new Timestamp(Calendar.getInstance().getTime().getTime()), getFileName(filename)) )
      .map( f => {
        val localPath = filePath.resolve(f.id + "." + suffix)
        uploadedFile.ref.moveTo(localPath , replace = true)
        import java.nio.file.attribute.PosixFilePermission._
        Files.setPosixFilePermissions(localPath, Set(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ) )
        Ok(Json.toJson(f))
      })

  }

  def deleteFile(fileId: Long) = deadbolt.SubjectPresent()() { req =>
    images.getFile(fileId).flatMap {
      case None => Future(NotFound("file not found"))
      case Some(file) => {
        images.deleteFile(file).map(count => {
          val filePath = Paths.get(config.get[String]("hear_us.files.km.folder")).resolve(file.kmId.toString).resolve(fileId.toString + "." + file.suffix)
          Files.delete(filePath)
          Ok(Json.toJson(count))
        })
      }
    }

  }

  //  def showUploadFile(id:Long) = Action { implicit req =>
  //    Ok( views.html.backoffice.uploadSitesFiles())
  //  }

  def apiFilesForKm( kmId:Long ) = Action.async{ req =>
    images.getFileForKM(kmId).map(image => Ok(Json.toJson(image)))
  }

  def apiUpdateCredit(id:Long) = deadbolt.SubjectPresent()(cc.parsers.tolerantText) { implicit req =>
        images.updateCredit(id, req.body).map(count => Ok(Json.toJson(count)))
  }

  def apiGetImage(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    images.getFile(id).map(image => Ok(Json.toJson(image)))
  }

  private def getFileSuffix (fileName:String): String = {
    var extension = ""
    val i = fileName.lastIndexOf('.')
    if (i > 0) extension = fileName.substring(i + 1)
    extension
  }

  private def getFileName (fileName:String): String = {
    var name = ""
    val i = fileName.lastIndexOf('.')
    if (i > 0) name = fileName.substring(0,i)
    name
  }
}
