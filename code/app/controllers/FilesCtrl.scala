package controllers

import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.sql.Timestamp
import java.util.Calendar

import javax.inject.Inject
import be.objectify.deadbolt.scala.DeadboltActions
import dataaccess.ImagesDAO
import dataaccess.JSONFormats._
import models.KMImage
import play.api.i18n._
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{ControllerComponents, InjectedController, PlayBodyParsers}
import play.api.{Configuration, Logger}

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class FilesCtrl @Inject() (images:ImagesDAO, cc:ControllerComponents, parsers:PlayBodyParsers,
                           config:Configuration, deadbolt:DeadboltActions, langs:Langs, messagesApi:MessagesApi) extends InjectedController {

  implicit private val ec: ExecutionContext = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }

  def apiAddFile(subjectId: String, subjectType:String) = deadbolt.SubjectPresent()(cc.parsers.multipartFormData ){ req =>
    val uploadedFile = req.body.files.head
    val filePath = Paths.get(config.get[String]("hearUs.files.mkImages.folder"))
    if ( ! Files.exists(filePath) ) {
      Files.createDirectories( filePath )
      Files.setPosixFilePermissions(filePath, Set(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ) )
    }
    val filename = req.body.dataParts("qqfilename").head
    val suffix = getFileSuffix(filename)
    
    Logger.info( s"got file $filename with suffix $suffix")
    Logger.info( "Uploaded: " + uploadedFile.toString  + " -- " + uploadedFile.ref.toString )
    val related = subjectType match {
      case "kms" => (Some(subjectId.toLong), None)
      case "camps" => (None, Some(subjectId.toLong))
    }
    val imageRec = KMImage(-1L, related._1, related._2, suffix,
                            uploadedFile.contentType.getOrElse(""),
                            new Timestamp(Calendar.getInstance().getTime.getTime),
                            getFileName(filename))
    images.storeImage( imageRec )
      .map(fileDbRecord => {
        val localPath = filePath.resolve(subjectId + "." + suffix)
        
        Logger.info("Moving file to " + localPath.toAbsolutePath.toString )
        val res = uploadedFile.ref.atomicMoveWithFallback(localPath)
        Logger.info( "res: " + res.toString)
        
        Files.setPosixFilePermissions(localPath, Set(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ) )
        // top object must contain a "success:true" field, for fineUploader to know the upload went well.
        Ok(Json.obj( "success"->true, "record"->Json.toJson(fileDbRecord)) )
      })

  }
  
  def getKmImage( id:Long ) = Action.async{ req =>
    images.getImageForKm(id).map({
      case None => NotFound("no km image")
      case Some( img ) => {
        val filePath = Paths.get(config.get[String]("hearUs.files.mkImages.folder"))
        Ok.sendPath(filePath.resolve(img.filename))
      }
    })
  }
  
  def getCampaignImage( id:Long ) = Action.async{ req =>
    images.getImageForCampaign(id).map({
      case None => NotFound("no campaign image")
      case Some( img ) => {
        val filePath = Paths.get(config.get[String]("hearUs.files.campaignImages.folder"))
        Ok.sendPath(filePath.resolve(img.filename))
      }
    })
  }
  
  def apiFilesForKm( kmId:Long ) = Action.async{ req =>
    images.getImageForKm(kmId).map(image => Ok(Json.toJson(image)))
  }
  
  def apiGetImage(id:Long) = deadbolt.SubjectPresent()() { implicit req =>
    images.getImageForKm(id).map(image => Ok(Json.toJson(image)))
  }

  private def getFileSuffix (fileName:String): String = {
    val i = fileName.lastIndexOf('.')
    if (i >= 0) fileName.substring(i + 1) else ""
  }

  private def getFileName (fileName:String): String = {
    val i = fileName.lastIndexOf('.')
    if (i > 0) fileName.substring(0,i) else fileName
  }

  def doAddImage(id: Long, subjectType:String) = deadbolt.SubjectPresent()(cc.parsers.multipartFormData) { implicit req =>
    Logger.info("do Add image " + subjectType)
    val file = req.body.file("imageFile")
    val imageCredit = req.body.dataParts.getOrElse("imageCredit", Seq[String]()).headOption.getOrElse("")
    val goTo = subjectType match {
      case "camps" => routes.CampaignMgrCtrl.editCampaign(id)
      case "kms" => routes.KnessetMemberCtrl.showEditKM(id)
    }
    if (file.isEmpty || file.get.filename.isEmpty) {
      // no file, but we might be able to update the image credit.
      images.getImage(id, subjectType).flatMap({
        case Some(imageRec) => {
          val updated = imageRec.copy(credit = imageCredit)
          images.storeImage(updated).map(_ => {
            val message = Informational(InformationalLevel.Success, Messages("knessetMemberEditor.imageCreditUpdated"))
            Redirect(goTo).flashing(FlashKeys.MESSAGE -> message.encoded)
          })
        }
        case None => {
          val message = Informational(InformationalLevel.Danger, Messages("knessetMemberEditor.imageFileMissing"))
          Future(Redirect(goTo).flashing(FlashKeys.MESSAGE -> message.encoded))
        }
      })

    } else {
      // We have a file. Update all.
      val filePart = file.get
      val folderPath = subjectType match {
        case "kms" => Paths.get(config.get[String]("hearUs.files.mkImages.folder"))
        case "camps" => Paths.get(config.get[String]("hearUs.files.camImages.folder"))
      }
      if (!Files.exists(folderPath)) {
        Files.createDirectories(folderPath)
        Files.setPosixFilePermissions(folderPath, Set(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ))
      }
      val i = filePart.filename.lastIndexOf('.')
      val suffix = if (i >= 0) filePart.filename.substring(i + 1) else ""
      val filePathTemp = folderPath.resolve(id.toString + ".-temp-." + suffix)
      val filePath = folderPath.resolve(id.toString + "." + suffix)
      filePart.ref.moveTo(filePathTemp, replace = true)
      Files.copy(filePathTemp, filePath, StandardCopyOption.REPLACE_EXISTING)
      Files.delete(filePathTemp)
      Files.setPosixFilePermissions(filePath, Set(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ))
      val mapped = subjectType match {
        case "camps" => (None, Some(id))
        case "kms" => (Some(id), None)
      }
      val imageRec = KMImage(-1L, mapped._1, mapped._2, suffix,
        filePart.contentType.getOrElse(""),
        new Timestamp(Calendar.getInstance().getTime.getTime),
        imageCredit)
      images.storeImage(imageRec).map(_ => {
        val message = Informational(InformationalLevel.Success, Messages("knessetMemberEditor.imageFileUploaded"))
        Redirect(goTo).flashing(FlashKeys.MESSAGE -> message.encoded)
      })
    }
  }
}
