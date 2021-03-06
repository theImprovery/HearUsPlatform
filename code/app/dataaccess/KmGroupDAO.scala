package dataaccess

import javax.inject.Inject
import models._
import play.api.{Configuration, Logger}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class KmGroupDAO @Inject() (protected val dbConfigProvider:DatabaseConfigProvider, conf:Configuration,
                            implicit val ec:ExecutionContext, knessetMembers:KnessetMemberDAO) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val groups = TableQuery[GroupTable]
  private val kmsGroups = TableQuery[KmGroupTable]
  private val camGroups = TableQuery[RelevantGroupTable]
  private val logger = Logger( classOf[KmGroupDAO] )
  
  def addGroup( group:KmGroup ):Future[KmGroup] = {
    val groupRow = KmGroupDN(group.id, group.name, group.knessetKey)
    if( group.id != 0 || group.id != -1 ) {
      Await.result(db.run {
        // remove group members
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

  def updateGroups( newGroups:Seq[KmGroup] ):Future[Seq[KmGroup]] = {
    logger.info("Updating groups: " + newGroups.map(_.knessetKey).mkString(","))
    db.run( DBIO.seq(kmsGroups.delete, groups.delete))
    Future.sequence(newGroups.map(gro => addGroup(gro)))
  }

  def getGroup( id:Long ):Future[Option[KmGroup]] = {
    val groupAndKms = for {
      (group, km) <- groups.filter(_.id === id) joinLeft kmsGroups on ( _.id === _.groupId )
    } yield (group, km)
    db.run {
      groupAndKms.result
    } map { resSeq => {
      val tupl = resSeq.headOption
      tupl.map(_._1).map( gr => KmGroup(gr.id, gr.name, gr.knessetKey, resSeq.map(_._2).map(_.get).map(_.kmId).toSet))
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

  def allGroupsDN(searchStr:Option[String]):Future[Seq[KmGroupDN]] = {
    db.run{
      searchStr match {
        case None => groups.result
        case Some(str) => groups.filter( row =>
          row.name.like(str)).result
      }
    }
  }

  def allGroups: Future[Seq[KmGroup]] = {
    val groupsAndKms = for {
      (group, km) <- groups joinLeft kmsGroups on (_.id === _.groupId)
    } yield (group, km)
    db.run {
      groupsAndKms.result
    } map { grSeq => {
      val rowsByGroup = grSeq.map( p => (p._1, p._2.get.kmId)).groupBy(_._1)
      rowsByGroup.map( g => {
        val gr:KmGroupDN = g._1
        KmGroup(gr.id, gr.name, gr.knessetKey, g._2.map(_._2).toSet)
      })
      }.toSeq
    }
  }
  
  /**
    * @return A map of (committee_id, Set[KM member ids)
    */
  def groupsMemberships( committees:Set[Long]):Future[Map[Long,Set[Long]]] = {
    db.run( kmsGroups.filter( _.groupId.inSet(committees) ).result ).map{ rows =>
      rows.groupBy( _.groupId ).map( p => (p._1, p._2.map(_.kmId).toSet) )
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

  def countGroups():Future[Int] = db.run(groups.size.result)

  def getGroupsForCampaign(id:Long): Future[Seq[KmGroupDN]] = {
    db.run(
      camGroups.join(groups).on((cg, g) => cg.groupId === g.id )
        .filter( _._1.camId ===id)
        .map( _._2).result
    )
  }

  def addGroupToCampaign(row:RelevantGroup) = db.run(camGroups += row)

  def removeGroupFromCamp(camId: Long, groupId: Long) = {
    db.run(
      camGroups.filter(r => r.camId === camId && r.groupId === groupId).delete
    )
  }

}
