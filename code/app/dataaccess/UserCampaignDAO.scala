package dataaccess

import javax.inject.Inject
import models.{Campaign, CampaignTeam, UserCampaign}
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserCampaignDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration
                                ) extends HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  private val userCampaign = TableQuery[UserCampaignTable]
  private val campaigns = TableQuery[CampaignTable]
  private val users = TableQuery[UserTable]
  
  /**
    * Returns the set of campaigns a user is involved in, and the
    * set of campaigns that she manages.
    * @param userId
    * @return
    */
  def getCampaignsForUser(userId:Long ):Future[(Set[Campaign], Set[Long])] = db.run(
    userCampaign.join(campaigns).on((uc, c) => uc.campaignId === c.id )
      .filter( _._1.userId ===userId ).result
  ).map( res => res.foldLeft((Set[Campaign](), Set[Long]()))( (res, row)=>{
    ( res._1 + row._2, if (row._1.isAdmin) res._2+row._1.campaignId else res._2 )
  }))
  
  def disconnectUserFromCampaign(userId:Long, campaignId:Long) = db.run{ userCampaign.filter(r => r.userId===userId && r.campaignId === campaignId).delete }
  
  def connectUserToCampaign( rel:UserCampaign ):Future[UserCampaign] = {
    db.run( (userCampaign returning userCampaign).insertOrUpdate(rel)
     ).map( insertRes => insertRes.getOrElse(rel) )
  }
  
  def removeFromTeam(userId:Long, campaignId:Long ): Future[Boolean] = {
    val plan = for {
      isAdmin <- userCampaign.filter( r => r.campaignId===campaignId && r.userId === userId && r.admin=== true).exists.result
      adminCount <- userCampaign.filter( r => r.campaignId===campaignId  && r.admin=== true).size.result
      removed <- {
        if (!isAdmin || adminCount > 1 ) {
          userCampaign.filter( r => r.campaignId===campaignId && r.userId === userId).delete
        } else DBIOAction.successful(0)
      }
    } yield (isAdmin, adminCount, removed)
    
    db.run( plan.transactionally ).map( res => res._3==1 )
  }
  
  def isUserOnCampaign( userId:Long, campaignId:Long ): Future[Boolean] = db.run(
    userCampaign.filter( r => r.userId===userId && r.campaignId===campaignId).exists.result
  )
  
  def isUserOnCampaign( userName:String, campaignId:Long ): Future[Boolean] = db.run(
    userCampaign.join(users).on((uc,u)=>uc.userId===u.id)
      .filter( r => r._2.username===userName && r._1.campaignId===campaignId)
      .exists.result
  )
  
  
  def removeAdminFromTeam(userId:Long, campaignId:Long ): Future[Boolean] = {
    val plan = for {
      adminCount <- userCampaign.filter( r => r.campaignId===campaignId  && r.admin=== true).size.result
      removed    <- {
        if (adminCount > 1) userCampaign.filter(r=>r.campaignId===campaignId && r.userId===userId ).map(_.admin).update(false)
        else DBIOAction.successful(0)
      }
    } yield removed
    
    db.run( plan.transactionally ).map( res => res==1 )
  }
  
  def getTeam( campaignId:Long ):Future[CampaignTeam]={
    val joined = users.join(userCampaign).on((u, uc)=>u.id === uc.userId ).filter(_._2.campaignId===campaignId)
    db.run( joined.result ).map( rows => {
      val collected = rows.map( r => (Set(r._1), Set(r._2)) ).reduce( (s1,s2)=>( s1._1 ++ s2._1, s1._2 ++ s2._2 ) )
      new CampaignTeam( collected._1, collected._2 )
    })
  }
  
}
