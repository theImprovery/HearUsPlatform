package dataaccess

import javax.inject.Inject
import models.{ContactOption, KnessetMember, Party}
import play.api.{Configuration, Logger}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class KnessetMemberDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val knessetMembers = TableQuery[KnessetMemberTable]
  private val parties = TableQuery[PartyTable]
  private val contactOptions = TableQuery[ContactOptionTable]

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

  def addContactOption( co: ContactOption ):Future[ContactOption] = {
    db.run{
      (contactOptions returning contactOptions).insertOrUpdate(co)
    }.map( insertRes => insertRes.getOrElse(co) )
  }

  def addContactOption( cos: Seq[ContactOption] ):Future[Seq[ContactOption]] = {
    Future.sequence(cos.map(co => addContactOption(co)))
  }

  def getContactOptions( kmId: Long ):Future[Seq[ContactOption]] = {
    db.run{
      contactOptions.filter( _.kmId === kmId ).result
    }
  }

  def deleteContactOption( kmId: Long, platform:Platform.Value ):Future[Int] = {
    db.run(contactOptions.filter(co => ( co.kmId === kmId ) && ( co.platform === platform.toString )).delete)
  }
  
  def countKMs: Future[Int] = db.run(knessetMembers.size.result)
  def countParties:Future[Int] = db.run(parties.size.result)
}

object Platform extends Enumeration {
  type Platform = Value
  val Phone, Email, Mail, Fax = Value
}
