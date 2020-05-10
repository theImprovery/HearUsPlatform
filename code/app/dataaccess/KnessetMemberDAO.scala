package dataaccess

import java.util.Date

import javax.inject.Inject
import models.{ContactOption, KmsParties, KnessetMember, Party, Platform}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class KnessetMemberDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val knessetMembers = TableQuery[KnessetMemberTable]
  private val parties = TableQuery[PartyTable]
  private val contactOptions = TableQuery[ContactOptionTable]
  private val kmsPartiesView = TableQuery[KmsPartiesView]
  
  def addKM( km:KnessetMember ):Future[KnessetMember] = {
    db.run {
      (knessetMembers returning knessetMembers).insertOrUpdate(km)
    }.map( insertRes => insertRes.getOrElse(km) )
  }

  def getKM( id:Long ):Future[Option[KnessetMember]] = {
    db.run{
      knessetMembers.filter( _.id === id ).result
    } map { _.headOption }
  }

  def getAllKms:Future[Seq[KnessetMember]] = db.run( knessetMembers.result )

  def prepareView(searchStr: Option[String], sortBy: SortBy.Value, isAsc: Boolean) = {
    val searchedView = searchStr match {
      case None => kmsPartiesView
      case Some(str) => kmsPartiesView.filter( row =>
        Seq( row.name.like(str), row.webPage.like(str), row.partyName.like(str), row.partyWebPage.like(str)
        ).reduceLeftOption(_||_).getOrElse(true:Rep[Boolean]) )
    }
    sortBy match {
      case SortBy.Parties => searchedView.sortBy(r => order(r.partyName, isAsc))
      case SortBy.KnessetMember => searchedView.sortBy(r => order(r.name, isAsc))
    }
  }

  def getKms(searchStr:Option[String], isAsc:Boolean, sortBy:SortBy.Value):Future[Seq[KmsParties]] = {
    db.run (
      prepareView(searchStr, sortBy, isAsc).filter( _.isActive===true ).result
    )
  }
  
  def deleteKM( id: Long ):Future[Unit] = {
    db.run{
      DBIO.seq(
        contactOptions.filter( _.kmId === id ).delete,
        knessetMembers.filter( _.id === id ).delete
      ).transactionally
    }
  }

  def addParty( party: Party ):Future[Party] = {
    db.run{
      (parties returning parties).insertOrUpdate(party)
    }.map( insertRes => insertRes.getOrElse(party) )
  }

  def getParty( id: Long ):Future[Option[Party]] = {
    db.run{
      parties.filter( _.id === id ).result
    }.map( _.headOption )
  }

  def getAllParties:Future[Seq[Party]] = db.run( parties.result )

  def deleteParty( id: Long ):Future[Int] = db.run(parties.filter( _.id === id ).delete)

  def updateParties(currentParties:Seq[Party] ) = {
    val markAsNotActive = parties.filter(party => !party.name.inSet(currentParties.map(_.name))).map(_.isActive).update(false)
    val markAsActive = parties.filter(party => party.name.inSet(currentParties.map(_.name))).map(_.isActive).update(true)
    val insertNewParties = for {
      existingPartyNames <- parties.map( _.name ).result
      addParties         <- parties ++= currentParties.filter( p => !existingPartyNames.contains(p.name) )
    } yield addParties
    db.run( DBIO.seq(markAsNotActive, markAsActive, insertNewParties).transactionally )
  }

  def updateKms(currentKms:Seq[KnessetMember]) = {
    // Mark KMs as active/inactive.
    val kmKeysForCurrentKnesset = currentKms.map(_.knessetKey)
    val markAsNotActive = knessetMembers.filter(km => !km.knessetKey.inSet(kmKeysForCurrentKnesset)).map(_.isActive).update(false)
    val markAsActive = knessetMembers.filter(km => km.knessetKey.inSet(kmKeysForCurrentKnesset)).map(_.isActive).update(true)
    val insertNewKms = for {
      existingKmsKeys <- knessetMembers.map( _.knessetKey ).result
      addKms          <- knessetMembers ++= currentKms.filter( k => !existingKmsKeys.contains(k.knessetKey))
    } yield addKms

    db.run(
      DBIO.seq(
        markAsNotActive,
        markAsActive,
        insertNewKms).transactionally )
    // update kms that appear in the Knesset OpenAPI, with data from that site.
    for {
      kmsSeq <- getAllKms
      kmsMap = kmsSeq.map( km =>km.knessetKey->km ).toMap
    } yield {
      val kmsToUpdate = currentKms.filter(_.knessetKey != -1L).map(km =>km.copy(id=kmsMap(km.knessetKey).id))
      val updateCommands = kmsToUpdate.map(km => knessetMembers.insertOrUpdate(km))
      db.run( DBIO.sequence(updateCommands))
    }
  }

  def getAllActiveParties():Future[Seq[Party]] = {
    db.run{
      parties.filter( _.isActive ).result
    }
  }

  def getAllActiveKms():Future[Seq[KnessetMember]] = {
    db.run{
      knessetMembers.filter( _.isActive ).result
    }
  }

  def addContactOption( co: ContactOption ):Future[ContactOption] = {
    db.run(contactOptions += co).map(_ => co)
  }

  def addContactOption( cos: Iterable[ContactOption] ):Future[Seq[ContactOption]] = {
    Future.sequence(cos.map(co => addContactOption(co)).toSeq)
  }

  def getAllContactOptions():Future[Seq[ContactOption]] = db.run(contactOptions.result)
  
  def setContactOptions( kmId:Long, conOps: Iterable[ContactOption] ):Future[Unit] = {
    val connectedConOps = conOps.map( c => c.copy(kmId=Some(kmId)) )
    val deleteCurrent = contactOptions.filter( _.kmId === kmId ).delete
    val insertNew     = DBIO.sequence( connectedConOps.map( contactOptions.insertOrUpdate ) )
    db.run{
      DBIO.seq( deleteCurrent, insertNew ).transactionally
    }.map( _ => () )
  }

  def getContactOptions( kmId: Long ):Future[Seq[ContactOption]] = {
    db.run{
      contactOptions.filter( _.kmId === kmId ).result
    }
  }

  def deleteContactOption( kmId: Long, platform:Platform.Value ):Future[Int] = {
    db.run(contactOptions.filter(co => ( co.kmId === kmId ) && ( co.platform === platform.toString )).delete)
  }
  
  def countKMs: Future[Int] = db.run(knessetMembers.filter(_.isActive).size.result)
  def countParties:Future[Int] = db.run(parties.filter(_.isActive).size.result)
  private def order( col:Rep[String], isAsc:Boolean ) = if ( isAsc ) col.asc else col.desc
}

object SortBy extends Enumeration {
  val Parties = Value("parties")
  val KnessetMember = Value("knesset_member")
}