package dataaccess

import javax.inject.Inject
import models.{CampaignTeam, UserCampaign}
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserCampaignDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration
                                ) extends HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  private val userCampaign = TableQuery[UserCampaignTable]
  private val campaigns = TableQuery[CampaignTable]
  private val users = TableQuery[UserTable]
  
  def getCampaginsForUser( userId:Long ) = db.run(
    userCampaign.join(campaigns).on((uc, c) => uc.campaignId === c.id )
      .filter( _._1.userId ===userId )
      .map( _._2).result
  )
  
  def disconnectUserFromCampaign(userId:Long, campaignId:Long) = db.run{ userCampaign.filter(r => r.userId===userId && r.campaignId === campaignId).delete }
  
  def connectUserToCampaign( rel:UserCampaign ):Future[UserCampaign] = {
    db.run( (userCampaign returning userCampaign).insertOrUpdate(rel)
     ).map( insertRes => insertRes.getOrElse(rel) )
  }
  
  def getTeam( campaignId:Long ):Future[CampaignTeam]={
    val joined = users.join(userCampaign).on((u, uc)=>u.id === uc.userId ).filter(_._2.campaignId===campaignId)
    db.run( joined.result ).map( rows => {
      val collected = rows.map( r => (Set(r._1), Set(r._2)) ).reduce( (s1,s2)=>( s1._1 ++ s2._1, s1._2 ++ s2._2 ) )
      new CampaignTeam( collected._1, collected._2 )
    })
  }
  
}
