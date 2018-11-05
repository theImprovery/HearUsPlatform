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
  
  def getImage(kmId:Long ):Future[Option[KMImage]] = {
    db.run {
      images.filter( _.kmId === kmId ).result
    } map { _.headOption}
  }

  def deleteImage( kmId:Long):Future[Int] = {
    db.run {
      images.filter(_.kmId === kmId).delete
    }
  }
  
}