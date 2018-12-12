package dataaccess

import javax.inject.Inject
import models.KMImage
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ImagesDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val images = TableQuery[ImageTable]

  def storeImage(anImage:KMImage ):Future[KMImage] = {
    db.run( images.insertOrUpdate(anImage) ).map( _ => anImage )
  }
  
  def getImageForKm(kmId:Long ):Future[Option[KMImage]] = {
    db.run {
      images.filter( _.kmId === kmId ).result
    } map { _.headOption}
  }

  def getImageForCamps(camId:Long ):Future[Option[KMImage]] = {
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

  def deleteImage( kmId:Long):Future[Int] = {
    db.run {
      images.filter(_.kmId === kmId).delete
    }
  }
  
}