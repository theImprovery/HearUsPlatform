package dataaccess

import javax.inject.Inject
import models.InteractionRecord
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class InteractionRecordDAO  @Inject() (protected val dbConfigProvider:DatabaseConfigProvider,
                                       ec:ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  val interactions = TableQuery[InteractionRecordTable]
  private implicit val iec:ExecutionContext = ec
  
  def store( icn: InteractionRecord ):Future[Boolean] = {
    db.run( interactions += icn ).map( _ == 1)
  }
  
}
