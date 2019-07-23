package dataaccess

import javax.inject.Inject

import models._
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class CampaignDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val campaigns = TableQuery[CampaignTable]
  private val labels = TableQuery[LabelTextTable]
  private val messages = TableQuery[CannedMessageTable]
  private val socialMedia = TableQuery[SocialMediaTable]
  private val positions = TableQuery[KmPositionTable]
  private val actions = TableQuery[KmActionTable]
  private val usersCampaigns = TableQuery[UserCampaignTable]
  private val texts = TableQuery[CampaignTextTable]

  def getAllCampaigns:Future[Seq[Campaign]] = db.run( campaigns.result )
  
  def count:Future[Int] = db.run( campaigns.size.result )
  
  def store(cam: Campaign ):Future[Campaign] = {
    db.run(
      (campaigns returning campaigns).insertOrUpdate(cam)
    ).map( insertRes => {
      val res = insertRes.getOrElse(cam)
      val ct = CampaignText(insertRes.getOrElse(cam).id, "", "", "", "", "","")
      updateTexts(ct)
      res
    } )
  }
  
  def updateDetails( id:Long, details:CampaignDetails ) = {
    db.run(
      campaigns.filter( _.id === id )
               .map(row => (row.title, row.slogan, row.contactEmail, row.website, row.analytics) )
               .update( (details.title, details.slogan, details.contactEmail, details.website, details.analyticsCode) )
    )
  }
  
  def updateDesign( id:Long, design:String ):Future[Boolean] = {
    db.run {
      campaigns.filter( _.id === id ).map( _.themeData ).update(design)
    }.map( res => res > 0 )
  }
  
  def getCampaign( id:Long ):Future[Option[Campaign]] = {
    db.run{
      campaigns.filter( _.id === id ).result
    } map ( _.headOption )
  }

  def deleteCampaign( id:Long ):Future[Unit] = {
    db.run(
      DBIO.seq(
        texts.filter( _.campaignId === id ).delete,
        campaigns.filter( _ .id === id ).delete
      ).transactionally
    )
  }

  def getLabelText( id:Long ):Future[Seq[LabelText]] = {
    db.run (labels.filter( _.camId === id ).result)
  }

  def addLabelText(lt: LabelText ):Future[LabelText] = {
    db.run{
      (labels returning labels).insertOrUpdate(lt)
    }.map( insertRes => insertRes.getOrElse(lt) )
  }

  def addLabelTexts(lts: Seq[LabelText] ):Future[Seq[LabelText]] = {
    db.run( labels.delete )
    Future.sequence(lts.map(lt => addLabelText(lt)))
  }

  def getMessages(campaignId:Long):Future[Seq[CannedMessage]] = {
    db.run (messages.filter( _.camId === campaignId ).result)
  }

  def addMessage(msg: CannedMessage ):Future[CannedMessage] = {
    db.run{
      (messages returning messages).insertOrUpdate(msg)
    }.map( insertRes => insertRes.getOrElse(msg) )
  }

  def setMessages(campaignId:Long, msgs: Seq[CannedMessage] ):Future[Unit] = {
    db.run(
      DBIO.seq(
        messages.filter( _.camId === campaignId ).delete,
        messages ++= msgs.map( m=>m.copy(camId=campaignId) )
      ).transactionally
    )
  }
  
  def getMessage(campaignId:Long, gender:Gender.Value, position:Position.Value):Future[Map[Platform.Value,CannedMessage]] = {
    import dataaccess.Mappers.positionMapper
    db.run(
      messages.filter( r => r.camId===campaignId && r.gender===gender.toString && r.position===position).result
    ).map( rows => rows.groupBy(_.platform).map( kv => (kv._1, kv._2.head) ) )
  }
  
  def getTextsFor( campaignId:Long ):Future[Option[CampaignText]] = db.run (
    texts.filter( _.campaignId === campaignId ).result
  ).map( _.headOption )
  
  def updateTexts( campaignTexts:CampaignText ): Future[Int] = db.run {
    texts.insertOrUpdate( campaignTexts )
  }
  
  def getSm( id:Long ):Future[Seq[SocialMedia]] = {
    db.run (socialMedia.filter( _.camId === id ).result)
  }

  def addSm(sm: SocialMedia ):Future[SocialMedia] = {
    db.run{
      (socialMedia returning socialMedia).insertOrUpdate(sm)
    }.map( insertRes => insertRes.getOrElse(sm) )
  }

  def addSm(sms: Seq[SocialMedia] ):Future[Seq[SocialMedia]] = {
    db.run( socialMedia.delete )
    Future.sequence(sms.map(sm => addSm(sm)))
  }

  def getPositions( camId:Long ):Future[Seq[KmPosition]] = {
    db.run( positions.filter( _.camId === camId ).result)
  }
  
  def getPosition( camId:Long, kmId:Long ):Future[Option[KmPosition]] = {
    db.run( positions.filter( r => (r.camId===camId) && (r.kmId===kmId) ).result).map(_.headOption)
  }
  
  def updatePosition( pos: KmPosition ):Future[KmPosition] = {
    db.run(
      (positions returning positions).insertOrUpdate(pos)
    ).map( insertRes => insertRes.getOrElse(pos) )
  }

  def getActions( camId:Long ):Future[Seq[KmAction]] = {
    db.run( actions.filter( _.camId === camId ).result)
  }

  def getActions( camId:Long, kmId:Long ):Future[Seq[KmAction]] = {
    db.run( actions.filter( a => (a.camId === camId) && (a.kmId === kmId)).sortBy(_.date.desc).result)
  }

  def getAction( id:Long ):Future[Option[KmAction]] = {
    db.run( actions.filter( _.id === id ).result).
      map( _.headOption )
  }

  def updateAction( action:KmAction ): Future[KmAction] = {
    db.run {
      (actions returning actions).insertOrUpdate(action)
    }.map( insertRes => insertRes.getOrElse(action) )
  }

  def deleteAction( id:Long ):Future[Int] = db.run( actions.filter( _.id === id ).delete )

  def isAllowToEdit( userId:Long, campaignId: Long ):Future[Boolean] = {
    db.run(
      usersCampaigns.filter(row => (row.userId === userId) && (row.campaignId === campaignId) ).result
    ) map ( _.nonEmpty)
  }

  def campaignSlugExists( name:String ):Future[Boolean] = {
    db.run{
      campaigns.map( _.slug ).filter( _.toLowerCase === name.toLowerCase() ).exists.result
    }
  }
  
  def getBySlug( slug:String ):Future[Option[Campaign]] = {
    db.run( campaigns.filter( _.slug.toLowerCase === slug.toLowerCase).result ).map( _.headOption )
  }

  def updatePublish( campId:Long, isPublish:Boolean ):Future[Int] = {
    db.run(
      campaigns.filter( _.id === campId ).map( _.isPublished ).update(isPublish)
    )
  }

}
