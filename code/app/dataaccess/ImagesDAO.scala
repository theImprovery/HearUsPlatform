package dataaccess


import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

import javax.inject.Inject
import models.KMImage
import play.api.{Configuration, Logger}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.Files.TemporaryFile
import slick.jdbc.JdbcProfile
import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{CopyOption, Files, Paths, StandardCopyOption}

import javax.imageio.ImageIO
import play.api.mvc.MultipartFormData

import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class ImagesDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val images = TableQuery[ImageTable]

  def storeImage(anImage:KMImage ):Future[KMImage] = {
    db.run( images.insertOrUpdate(anImage) ).map( _ => anImage )
  }
  
  def getImageForKm(kmId:Long ):Future[Option[KMImage]] = {
    db.run {
      images.filter( _.kmId === kmId ).result
    }.map(_.headOption)
  }

  def getImageForCampaign(camId:Long ):Future[Option[KMImage]] = {
    db.run {
      images.filter( _.camId === camId ).result
    } map { _.headOption}
  }

  def getImage( id:Long, subjectType:String ):Future[Option[KMImage]] = {
    db.run {
      subjectType match {
        case "kms" => images.filter( _.kmId === id ).result
        case "camps" => images.filter( _.camId === id ).result
      }
    } map { _.headOption}
  }
  
  def getAllKmImages:Future[Map[Long,KMImage]] = db.run {
    images.filter( _.kmId.nonEmpty ).result
  }.map( res => res.map(r=>(r.kmId.get,r)).toMap )

  def deleteImageRecord(id:Long):Future[Int] = {
    db.run {
      images.filter(_.id === id).delete
    }
  }
  
  def storeCampaignImageFile( campaignId:Long, filePart:MultipartFormData.FilePart[TemporaryFile] ):Unit = {
    import scala.concurrent.duration._
    Logger.info("Storing image for campaign %d: %s (%s)".format(campaignId, filePart.filename, filePart.contentType.toString))
    val folderPath = Paths.get(conf.get[String]("hearUs.files.campaignImages.folder"))
    if (!Files.exists(folderPath)) {
      Files.createDirectories(folderPath)
      Files.setPosixFilePermissions(folderPath, Set(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ))
    }
    
    val previousImage = Await.result(getImageForCampaign(campaignId), 30 seconds )
    previousImage.foreach( deleteCampaignImageFile )
    
    val suffix = filePart.filename.split("\\.").last
    val filePath = folderPath.resolve(campaignId.toString + "." + suffix)
  
    val inImage = ImageIO.read(filePart.ref.path.toFile)
    if ( inImage.getWidth() > 1000 ) {
      // resize
      val ratio = 1000.0/ddinImage.getWidth
      val newImage = resizeImage(inImage, 1000, (inImage.getHeight*ratio).toInt)
      ImageIO.write( newImage, suffix, filePath.toFile )
    } else {
      // direct copy
      filePart.ref.moveTo(filePath.toFile, replace = true)
    }
    
    controllers.Utils.ensureImageServerReadPermissions(filePath)
  }
  
  def deleteCampaignImageFile( image:KMImage ): Unit = {
    val folderPath = Paths.get(conf.get[String]("hearUs.files.campaignImages.folder"))
    if (Files.exists(folderPath)) {
      val path = folderPath.resolve(image.filename)
      if ( Files.exists(path) ) {
        Files.delete(path)
      }
    }
  }
  
  private def resizeImage(inImage:BufferedImage, destWidth:Int, destHeight:Int):BufferedImage = {
    val resized = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB)
      val scaler = AffineTransform.getScaleInstance(destWidth/inImage.getWidth.toDouble, destHeight/inImage.getHeight.toDouble)
      val g2 = resized.createGraphics()
      g2.setRenderingHints(Map(
        RenderingHints.KEY_RENDERING -> RenderingHints.VALUE_RENDER_QUALITY,
        RenderingHints.KEY_INTERPOLATION -> RenderingHints.VALUE_INTERPOLATION_BICUBIC,
        RenderingHints.KEY_FRACTIONALMETRICS -> RenderingHints.VALUE_FRACTIONALMETRICS_ON
      ))
      g2.drawImage(inImage, scaler, null)
      g2.dispose()
      resized
  }
  
}