package dataaccess

import javax.inject.Inject
import models.Campaign
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class CampaignDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  private val campaigns = TableQuery[CampaignTable]

  def getAllCampaigns:Future[Seq[Campaign]] = db.run( campaigns.result )

  def add( cam: Campaign ):Future[Campaign] = {
    db.run(
      (campaigns returning campaigns).insertOrUpdate(cam)
    ).map( insertRes => insertRes.getOrElse(cam) )
  }
}
