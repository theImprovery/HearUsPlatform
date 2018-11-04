package dataaccess

import javax.inject.Inject
import models.{GroupIdKmId, KmGroupDN, KmGroups, KnessetMember}
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class KmGroupDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration,
                            implicit val ec:ExecutionContext, knessetMembers:KnessetMemberDAO) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val groups = TableQuery[GroupTable]
  private val kmsGroups = TableQuery[KmGroupTable]

  def addGroup( group:KmGroups ):Future[KmGroups] = {
    val groupRow = KmGroupDN(group.id, group.name)
    if( group.id != 0 || group.id != -1 ) {
      Await.result(db.run {
        //delete stores group's kms
        kmsGroups.filter( _.groupId === group.id ).delete
      }, Duration.Inf)
    }
    db.run {
      //store group
      (groups returning groups).insertOrUpdate(groupRow)
    } flatMap { r =>
      val row = r.getOrElse(groupRow)
      //store all kms
      db.run {
        kmsGroups ++= group.kms.toSeq.map( km => GroupIdKmId(row.id, km))
      } map( _ => row)
    } map{ row =>
      //returning row with update id
      group.copy(id=row.id)
    }
  }

  def getGroup( id:Long ):Future[Option[KmGroups]] = {
    val groupAndKms = for {
      (group, km) <- groups.filter(_.id === id) joinLeft kmsGroups on ( _.id === _.groupId )
    } yield (group, km)
    db.run {
      groupAndKms.result
    } map { resSeq => {
      val tupl = resSeq.headOption
      tupl.map(_._1).map( gr => KmGroups(gr.id, gr.name, resSeq.map(_._2).map(_.get).map(_.kmId).toSet))
    }}
  }

  def getGroupDN( id:Long ):Future[Option[KmGroupDN]] = {
    db.run(
      groups.filter( _.id === id ).result
    ) map( _.headOption)
  }

  def getKmForGroup( id:Long ):Future[Seq[Long]] = {
    db.run(
      kmsGroups.filter( _.groupId === id).result
    ) map( s => s.map( _.kmId ))
  }

  def allGroupsDN:Future[Seq[KmGroupDN]] = db.run( groups.result )

  def allGroups: Future[Seq[KmGroups]] = {
    val groupsAndKms = for {
      (group, km) <- groups joinLeft kmsGroups on (_.id === _.groupId)
    } yield (group, km)
    db.run {
      groupsAndKms.result
    } map { grSeq => {
      val rowsByGroup = grSeq.map( p => (p._1, p._2.get.kmId)).groupBy(_._1)
      rowsByGroup.map( g => {
        val gr:KmGroupDN = g._1
        KmGroups(gr.id, gr.name, g._2.map(_._2).toSet)
      })
      }.toSeq
    }
  }

  def deleteGroup( id:Long ):Future[Int] = {
    db.run{
      kmsGroups.filter( _.groupId === id ).delete
    } flatMap { _ =>
      db.run {
        groups.filter( _.id === id).delete
      }
    }
  }

  private def idsToSet( ids:Iterable[Long] ):Set[KnessetMember] = {
    val res = Future.sequence(ids.map(knessetMembers.getKM).toSet)
    Await.result(res.map( fokm => fokm.flatten ), 10 seconds)
  }
}
