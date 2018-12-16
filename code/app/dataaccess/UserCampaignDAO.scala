package dataaccess

import javax.inject.Inject
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

class UserCampaignDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration
                                ) extends HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  private val UserCampaigns = TableQuery[UserCampaignTable]
  private val Campaigns = TableQuery[CampaignTable]
  private val Users = TableQuery[UserTable]
  
  def connect(userId:Long, campaignId:Long) = db.run{ UserCampaigns += UserCampaign(userId, campaignId) }
  def disconnect(userId:Long, campaignId:Long) = db.run{ UserCampaigns.filter( r => r.userId===userId && r.campaignId === campaignId).delete }
  
  def getCampaginsForUser( userId:Long ) = db.run(
    UserCampaigns.join(Campaigns).on( (uc, c) => uc.campaignId === c.id )
      .filter( _._1.userId ===userId )
      .map( _._2).result
  )
  
  def getUsersForCampaign(campaignId:Long ) = db.run(
    UserCampaigns.join(Users).on( (uc, u) => uc.userId === u.id )
      .filter( _._1.campaignId ===campaignId )
      .map( _._2).result
  )
  
}
