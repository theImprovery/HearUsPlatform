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

  def addFile( i:KMImage ):Future[KMImage] = {
    db.run( images.returning(images.map(_.id))
      .into((file, newId) => file.copy(id=newId)) += i)
  }

  def getFile( id:Long ):Future[Option[KMImage]] = {
    db.run {
      images.filter( _.id === id ).result
    } map { _.headOption}
  }

  def getFileForKM( kmId:Long ):Future[Option[KMImage]] = {
    db.run {
      images.filter( _.kmId === kmId ).result
    } map { _.headOption}
  }

  def updateFile( i:KMImage ):Future[KMImage] = {
    db.run {
      images.filter(_.id === i.id).update(i)
    } map { _=> i}
  }

  def deleteFile( i:KMImage ):Future[Int] = {
    db.run {
      images.filter(_.id === i.id).delete
    }
  }

  def updateCredit( id:Long, credit:String ): Future[Int] = {
    db.run {
      images.filter( _.id === id).map( _.credit).update(credit)
    }
  }
}