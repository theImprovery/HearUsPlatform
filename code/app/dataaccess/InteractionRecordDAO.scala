package dataaccess

import javax.inject.Inject
import models.{InteractionDetails, InteractionRecord, InteractionSummary}
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class InteractionRecordDAO  @Inject() (protected val dbConfigProvider:DatabaseConfigProvider,
                                       ec:ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  
  val interactions = TableQuery[InteractionRecordTable]
  val summaries = TableQuery[InteractionSummaryView]
  val details   = TableQuery[InteractionDetailsView]
  
  private implicit val iec:ExecutionContext = ec
  
  def store( icn: InteractionRecord ):Future[Boolean] = {
    db.run( interactions += icn ).map( _ == 1)
  }
  
  def summaryForCampaign( campId:Long ): Future[Seq[InteractionSummary]] = db.run(
    summaries.filter( _.campaignId === campId ).sortBy( _.medium ).result
  )
  
  def detailsForCampaign( campId:Long ): Future[Seq[InteractionDetails]] = db.run (
    details.filter( _.campaignId === campId ).sortBy( _.time ).result
  )
}
