package controllers

import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.{BufferedImage, ByteLookupTable, LookupOp, ShortLookupTable}
import java.io.{ByteArrayOutputStream, IOException}
import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.sql.Timestamp
import java.util.Calendar

import javax.inject.Inject
import be.objectify.deadbolt.scala.DeadboltActions
import dataaccess.ImagesDAO
import dataaccess.JSONFormats._
import javax.imageio.ImageIO
import models.KMImage
import play.api.i18n._
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{ControllerComponents, InjectedController, PlayBodyParsers}
import play.api.{Configuration, Environment, Logger}

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FilesCtrl @Inject() (images:ImagesDAO, cc:ControllerComponents, parsers:PlayBodyParsers, env:Environment,
                           config:Configuration, deadbolt:DeadboltActions, langs:Langs, messagesApi:MessagesApi) extends InjectedController {

  implicit private val ec: ExecutionContext = cc.executionContext
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  val logger = Logger(classOf[FilesCtrl])

  def apiAddFile(subjectId: String, subjectType:String) = deadbolt.SubjectPresent()(cc.parsers.multipartFormData){ req =>
    val uploadedFile = req.body.files.head
    val filePath = Paths.get(config.get[String]("hearUs.files.mkImages.folder"))
    if ( ! Files.exists(filePath) ) {
      Files.createDirectories(filePath)
      Utils.ensureImageServerReadPermissions(filePath)
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
        
        Utils.ensureImageServerReadPermissions(localPath)
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
    logger.info(s"do Add image '$subjectType'")
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
      logger.info("Updating file")
      val filePart = file.get
      val folderPath = subjectType match {
        case "kms" => Paths.get(config.get[String]("hearUs.files.mkImages.folder"))
        case "camps" => Paths.get(config.get[String]("hearUs.files.camImages.folder"))
      }
      if (!Files.exists(folderPath)) {
        Files.createDirectories(folderPath)
        Utils.ensureImageServerReadPermissions(folderPath)
      }
      
      var suffix:String=""
      var contentType = filePart.contentType.getOrElse("")
      
      if ( subjectType == "kms" ) {
        // KM Images
        val dest = folderPath.resolve(id.toString + ".png")
        processKmImageFile(filePart.ref.path, dest ) match {
          case Success(p) => {
            Utils.ensureImageServerReadPermissions(p)
            suffix = "png"
            contentType = "image/png"
          }
          case Failure(e) => logger.warn("Error while saving KM image: " + e.getMessage, e)
        }
        
      } else {
        // Campaign Images
        val i = filePart.filename.lastIndexOf('.')
        suffix = if (i >= 0) filePart.filename.substring(i + 1) else "jpg"
        val filePath = folderPath.resolve(id.toString + "." + suffix)
        processCampaignImageFile(filePart.ref.path, filePath, suffix) match {
          case Success(p) => {
            Utils.ensureImageServerReadPermissions(p)
          }
          case Failure(iox) => logger.warn("Error while saving KM image: " + iox.getMessage, iox)
        }
      }
      
      // update database
      val imageId = subjectType match {
        case "camps" => (None, Some(id))
        case "kms" => (Some(id), None)
      }
      val imageRec = KMImage(-1L, imageId._1, imageId._2, suffix,
        contentType,
        new Timestamp(Calendar.getInstance().getTime.getTime),
        imageCredit)
      images.storeImage(imageRec).map(_ => {
        val message = Informational(InformationalLevel.Success, Messages("knessetMemberEditor.imageFileUploaded"))
        Redirect(goTo).flashing(FlashKeys.MESSAGE -> message.encoded)
      })
    }
  }
  
  def sampleImageTest = Action{
    val imgFile = env.getExistingFile("public/images/sample-img.jpg").get
    val inImg = ImageIO.read(imgFile)
    
    val outImg = processKmImage(inImg)
    
    val byteStream = new ByteArrayOutputStream()
    ImageIO.write(outImg, "png", byteStream)
    byteStream.close()
    Ok( byteStream.toByteArray ).as("image/png")
  }
  
  def processCampaignImageFile(imagePath:Path, destination:Path, format:String):Try[Path] = {
    try {
      val inImage = ImageIO.read(imagePath.toFile)
      logger.info(s"campaign image width: ${inImage.getWidth}")
      if ( inImage.getWidth() > 1000 ) {
        // resize
        val ratio = inImage.getWidth/1000.0
        val newImage = resizeImage(inImage, 1000, (inImage.getHeight*ratio).toInt, toGrayScale = false)
        logger.info("Resizing")
        ImageIO.write( newImage, format, destination.toFile )
      } else {
        // direct copy
        Files.copy(imagePath, destination, StandardCopyOption.REPLACE_EXISTING)
      }
      Success(destination)
    } catch {
      case iox:IOException => Failure(iox)
    }
  }
  
  def processKmImageFile ( imagePath:Path, destination:Path ):Try[Path] = {
    try {
      val inImage = ImageIO.read(imagePath.toFile)
      val otImage = processKmImage(inImage)
      ImageIO.write( otImage, "png", destination.toFile )
      Success(destination)
    } catch {
      case iox:IOException => Failure(iox)
    }
  }
  
  val KM_IMAGE_SIDE = 100
  def processKmImage( inImage:BufferedImage ):BufferedImage = {
    stretchHistogram(resizeImage(inImage, KM_IMAGE_SIDE, KM_IMAGE_SIDE, toGrayScale = true))
  }
  
  def resizeImage( inImage:BufferedImage, destWidth:Int, destHeight:Int, toGrayScale:Boolean):BufferedImage = {
    val resized = new BufferedImage(destWidth, destHeight,
                                        if ( toGrayScale ) BufferedImage.TYPE_BYTE_GRAY else  BufferedImage.TYPE_INT_RGB)
    val g2 =  resized.createGraphics()
    g2.setRenderingHints(Map(
      RenderingHints.KEY_INTERPOLATION -> RenderingHints.VALUE_INTERPOLATION_BICUBIC,
      RenderingHints.KEY_RENDERING -> RenderingHints.VALUE_RENDER_QUALITY,
      RenderingHints.KEY_FRACTIONALMETRICS -> RenderingHints.VALUE_FRACTIONALMETRICS_ON
    ))
    val scaler = AffineTransform.getScaleInstance(destWidth/inImage.getWidth.toDouble, destHeight/inImage.getHeight.toDouble)
    g2.drawImage(inImage, scaler, null)
    g2.dispose()
    
    resized
  }
  
  def stretchHistogram(inImage:BufferedImage):BufferedImage = {
    val minMax = Range(0,inImage.getWidth).flatMap(x=>Range(0,inImage.getHeight).map(y=>(x,y)))
      .map( crd=>inImage.getRGB(crd._1, crd._2) )
      .map( _ & 0xFF ) // take just one component, we're grayscale anyway
      .foldLeft((255,0)) { case ((min, max), e) => (math.min(min, e), math.max(max, e))}
  
    val min = minMax._1
    val max = minMax._2
    val factor = 255.0/(max-min).toDouble
    val lookupBytes = Range(0,256).map( i => (i-min)*factor ).map(x=>math.max(0,math.min(255,x))).map(_.toByte).toArray
    val lookupTable = new ByteLookupTable(0, lookupBytes)
    val stretchOp = new LookupOp( lookupTable, new RenderingHints(Map(
      RenderingHints.KEY_COLOR_RENDERING->RenderingHints.VALUE_COLOR_RENDER_QUALITY
    )))
    
    stretchOp.filter(inImage, null)
  }
}
