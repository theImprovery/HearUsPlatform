package dataaccess

import java.sql.Timestamp

import models.{Party, _}
import slick.ast.ColumnOption.AutoInc
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

object Mappers {
  implicit val positionMapper = MappedColumnType.base[Position.Value, String](
    (p:Position.Value) => p.toString,
    (s:String) => Position.withName(s)
  )
  implicit val platformMappers = MappedColumnType.base[Platform.Value, String](
    (p:Platform.Value) => p.toString,
    (s:String) => Platform.withName(s)
  )
  implicit val typeMappers = MappedColumnType.base[ActionType.Value, String](
    (a:ActionType.Value) => a.toString,
    (s:String) => ActionType.withName(s)
  )
  implicit val roleSeqMappers = MappedColumnType.base[Set[UserRole.Value], Int](
    (a:Set[UserRole.Value]) => a.map(r => Math.pow(2,r.id).toInt ).sum,
    (i:Int) => UserRole.values.map(r => (r, (i>>r.id) & 1) ).filter(_._2==1).map(_._1)
  )
}

case class UserCampaign(userId:Long, campaignId:Long)

class UserTable(tag:Tag) extends Table[User](tag,"users") {
  import Mappers.roleSeqMappers
  def id                = column[Long]("id",O.PrimaryKey, O.AutoInc)
  def username          = column[String]("username")
  def name              = column[String]("name")
  def email             = column[String]("email")
  def userRoles         = column[Set[UserRole.Value]]("roles")
  def encryptedPassword = column[String]("encrypted_password")

  def * = (id, username, name, email, userRoles, encryptedPassword) <> (User.tupled, User.unapply)
}

class UserCampaignTable(tag:Tag) extends Table[UserCampaign](tag,"user_campaign") {
  def userId     = column[Long]("user_id", O.PrimaryKey                                       )
  def campaignId = column[Long]("campaign_id", O.PrimaryKey)
  
  def * = (userId, campaignId) <> (UserCampaign.tupled, UserCampaign.unapply)
  
  def userFK = foreignKey("user_campaign_user_id_fkey", userId, TableClasses.knessetMembers)(_.id)
  def campaignFK = foreignKey("user_campaign_campaign_id_fkey", userId, TableClasses.campaigns)(_.id)
}

class InvitationTable(tag:Tag) extends Table[Invitation](tag, "invitations") {
  def email = column[String]("email", O.PrimaryKey)
  def date  = column[Timestamp]("date")
  def uuid  = column[String]("uuid")
  def sender  = column[String]("sender",O.PrimaryKey)

  def * = (email, date, uuid, sender) <> (Invitation.tupled, Invitation.unapply)
}
class PasswordResetRequestTable(tag:Tag) extends Table[PasswordResetRequest](tag, "password_reset_requests"){
  def username = column[String]("username")
  def uuid     = column[String]("uuid")
  def reset_password_date = column[Timestamp]("reset_password_date")

  def pk = primaryKey("uuid_for_forgot_password_pkey", (username, uuid))

  def * = (username, uuid, reset_password_date) <> (PasswordResetRequest.tupled, PasswordResetRequest.unapply)
}

class KnessetMemberTable(tag:Tag) extends Table[KnessetMember](tag,"knesset_members"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def gender = column[String]("gender")
  def isActive = column[Boolean]("is_active")
  def webPage = column[String]("web_page")
  def partyId = column[Long]("party_id")
  def knessetKey = column[Long]("knesset_key")

  def partyFK = foreignKey("knesset_member_party_fkey", partyId, TableClasses.parties)(_.id)
  def * = (id, name, gender, isActive, webPage, partyId, knessetKey) <> (KnessetMember.tupled, KnessetMember.unapply)
}

class ContactOptionTable(tag:Tag) extends Table[ContactOption](tag, "contact_options"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def kmId = column[Option[Long]]("km_id")
  def campaignId = column[Option[Long]]("campaign_id")
  def platform = column[String]("platform")
  def title = column[String]("title")
  def details = column[String]("details")
  def note = column[String]("note")

  def kmFK = foreignKey("knesset_member_contact_fkey", kmId, TableClasses.knessetMembers)(_.id.?)
  def campaignFK = foreignKey("campaign_fkey", campaignId, TableClasses.campaigns)(_.id.?)
  def * = (id, kmId, campaignId, platform, title, details, note) <> (ContactOption.tupled, ContactOption.unapply)
}

class PartyTable(tag:Tag) extends Table[Party](tag, "parties") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def webPage = column[String]("web_page")
  def isActive = column[Boolean]("is_active")

  def * = (id, name, webPage, isActive) <> (Party.tupled, Party.unapply)
}

class ImageTable(tag:Tag) extends Table[KMImage](tag, "images") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def kmId = column[Option[Long]]("km_id")
  def camId = column[Option[Long]]("cam_id")
  def suffix = column[String]("suffix")
  def mimeType = column[String]("mime_type")
  def date = column[Timestamp]("date")
  def credit = column[String]("credit")

  def kmFK = foreignKey("image_knesset_member_fkey", kmId, TableClasses.knessetMembers)( c=> c.id.? )
  def * = (id, kmId, camId, suffix, mimeType, date, credit) <> (KMImage.tupled, KMImage.unapply)
}

class GroupTable(tag:Tag) extends Table[KmGroupDN](tag, "groups") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def knessetKey = column[Long]("knesset_key")

  def * = (id, name, knessetKey) <> (KmGroupDN.tupled, KmGroupDN.unapply)
}

class KmGroupTable( tag:Tag ) extends Table[GroupIdKmId](tag, "km_group") {
  def groupId = column[Long]("group_id", O.PrimaryKey)
  def kmId    = column[Long]("km_id", O.PrimaryKey)

  def * = (groupId, kmId) <> (GroupIdKmId.tupled, GroupIdKmId.unapply)

  def groups    = foreignKey("group_fkey", groupId, TableClasses.groups)(_.id)
  def kms = foreignKey("kms_fkey", kmId, TableClasses.knessetMembers)(_.id)
}

class KmsPartiesView( tag:Tag ) extends Table[KmsParties](tag, "kms_parties") {
  def id = column[Long]("id")
  def name = column[String]("name")
  def gender = column[String]("gender")
  def isActive = column[Boolean]("is_active")
  def webPage = column[String]("web_page")
  def partyId = column[Long]("party_id")
  def knessetKey = column[Long]("knesset_key")
  def partyName = column[String]("party_name")
  def partyWebPage = column[String]("party_web_page")
  def partyIsActive = column[Boolean]("party_is_active")

  def * = (id, name, gender, isActive, webPage, partyId, knessetKey, partyName, partyWebPage, partyIsActive) <> (
      { case (id, name, gender, isActive, webPage, partyId, knessetKey, partyName, partyWebPage, partyIsActive) =>
                    KmsParties(KnessetMember(id, name, gender, isActive, webPage, partyId, knessetKey), Party(partyId, partyName, partyWebPage, partyIsActive))},
      { kmp: KmsParties => Some(kmp.km.id, kmp.km.name, kmp.km.gender, kmp.km.isActive,
                    kmp.km.webPage, kmp.km.partyId, kmp.km.knessetKey, kmp.party.name, kmp.party.webPage, kmp.party.isActive)}
  )
}

class CampaignTable( tag:Tag ) extends Table[Campaign](tag, "campaigns") {
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def title = column[String]("title")
  def slogan = column[String]("slogan")
  def slug   = column[String]("slug")
  def website = column[String]("website")
  def themeData = column[String]("theme_data")
  def contactEmail = column[String]("contact_email")
  def analytics = column[String]("analytics_code")
  def isPublished = column[Boolean]("is_published")

  def * = (id, title, slogan, slug, website, themeData, contactEmail, analytics, isPublished
            ) <> (Campaign.tupled, Campaign.unapply)
}

class CampaignTextTable( tag:Tag ) extends Table[CampaignText](tag, "campaign_texts") {
  def campaignId = column[Long]("campaign_id", O.PrimaryKey)
  def title = column[String]("title")
  def subtitle = column[String]("subtitle")
  def bodyText = column[String]("body_text")
  def footer   = column[String]("footer")
  def groupLabels = column[String]("group_labels")
  def mkLabels = column[String]("km_labels")
  
  def * = (campaignId, title, subtitle, bodyText, footer, groupLabels, mkLabels
            )<>(CampaignText.tupled, CampaignText.unapply)
  def fkCampaignId = foreignKey("campaign_texts_campaign_id_fkey", campaignId, TableClasses.campaigns)(_.id)
}

class LabelTextTable( tag:Tag ) extends Table[LabelText](tag, "label_texts") {
  import Mappers.positionMapper
  def camId = column[Long]("cam_id", O.PrimaryKey)
  def position = column[Position.Value]("position", O.PrimaryKey)
  def gender = column[String]("gender", O.PrimaryKey)
  def text = column[String]("text")

  def camps    = foreignKey("camps_fkey", camId, TableClasses.campaigns)(_.id)
  def * = (camId, position, gender, text) <> (LabelText.tupled, LabelText.unapply)
}

class RelevantGroupTable( tag:Tag ) extends Table[RelevantGroup](tag, "relevant_groups") {
  def camId    = column[Long]("cam_id", O.PrimaryKey)
  def groupId = column[Long]("group_id", O.PrimaryKey)

  def camps = foreignKey("camps_fkey", camId, TableClasses.campaigns)(_.id)
  def groups    = foreignKey("group_fkey", groupId, TableClasses.groups)(_.id)
  def * = (camId, groupId) <> (RelevantGroup.tupled, RelevantGroup.unapply)
}

class CannedMessageTable( tag:Tag ) extends Table[CannedMessage](tag, "canned_messages") {
  import Mappers._
  def camId = column[Long]("cam_id", O.PrimaryKey)
  def position = column[Position.Value]("position", O.PrimaryKey)
  def gender = column[String]("gender", O.PrimaryKey)
  def platform = column[Platform.Value]("platform", O.PrimaryKey)
  def text = column[String]("text")

  def camp = foreignKey("camp_fkey", camId, TableClasses.campaigns)(_.id)
  def * = (camId, position, gender, platform, text) <> (CannedMessage.tupled, CannedMessage.unapply)
}

class SocialMediaTable( tag:Tag ) extends Table[SocialMedia](tag, "social_media") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def camId = column[Long]("cam_id")
  def name = column[String]("name")
  def service = column[String]("service")

  def camp = foreignKey("camp_fkey", camId, TableClasses.campaigns)(_.id)
  def * = (id, camId, name, service) <> (SocialMedia.tupled, SocialMedia.unapply)
}

class KmPositionTable( tag:Tag ) extends Table[KmPosition](tag, "km_positions") {
  import Mappers.positionMapper
  def kmId = column[Long]("km_id", O.PrimaryKey)
  def camId = column[Long]("cam_id", O.PrimaryKey)
  def position = column[Position.Value]("position")

  def camp = foreignKey("camp_fkey", camId, TableClasses.campaigns)(_.id)
  def km    = foreignKey("km_fkey", kmId, TableClasses.knessetMembers)(_.id)
  def * = (kmId, camId, position) <> (KmPosition.tupled, KmPosition.unapply)
}

class KmActionTable( tag:Tag ) extends Table[KmAction](tag, "km_actions") {
  import Mappers.typeMappers
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def camId = column[Long]("cam_id")
  def kmId = column[Long]("km_id")
  def actionType = column[ActionType.Value]("type")
  def date = column[Timestamp]("date")
  def title = column[String]("title")
  def details = column[String]("details")
  def link = column[String]("link")

  def camp = foreignKey("camp_fkey", camId, TableClasses.campaigns)(_.id)
  def km = foreignKey("km_fkey", kmId, TableClasses.knessetMembers)(_.id)
  def * = (id, camId, kmId, actionType, date, title, details, link) <> (KmAction.tupled, KmAction.unapply)
}

class SystemEventTable(tag:Tag ) extends Table[SystemEvent](tag, "system_events") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("user_id")
  def date = column[Timestamp]("date")
  def message = column[String]("message")
  def details = column[String]("details")

  def * = (id, userId, date, message, details) <> (SystemEvent.tupled, SystemEvent.unapply)
}

object TableClasses {
  val parties = TableQuery[PartyTable]
  val knessetMembers = TableQuery[KnessetMemberTable]
  val groups = TableQuery[GroupTable]
  val campaigns = TableQuery[CampaignTable]
}