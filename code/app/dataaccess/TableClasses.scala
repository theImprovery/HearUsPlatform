package dataaccess

import java.sql.Timestamp

import models.{Party, _}
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._


class UserTable(tag:Tag) extends Table[User](tag,"users") {

  def id                = column[Long]("id",O.PrimaryKey, O.AutoInc)
  def username          = column[String]("username")
  def name              = column[String]("name")
  def email             = column[String]("email")
  def encryptedPassword = column[String]("encrypted_password")

  def * = (id, username, name, email, encryptedPassword) <> (User.tupled, User.unapply)

}

class InvitationTable(tag:Tag) extends Table[Invitation](tag, "invitations") {
  def email = column[String]("email", O.PrimaryKey)
  def date  = column[Timestamp]("date")
  def uuid  = column[String]("uuid")
  def sender  = column[String]("sender",O.PrimaryKey)

//  def pk = primaryKey("invitation_pkey", (email, sender))

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

  def patryFK = foreignKey("knesset_member_party_fkey", partyId, TableClasses.parties)(_.id)
  def * = (id, name, gender, isActive, webPage, partyId) <> (KnessetMember.tupled, KnessetMember.unapply)
}

class ContactOptionTable(tag:Tag) extends Table[ContactOption](tag, "contact_options"){
  def kmId = column[Long]("km_id", O.PrimaryKey)
  def platform = column[String]("platform", O.PrimaryKey)
  def title = column[String]("title")
  def details = column[String]("details")
  def note = column[String]("note")

  def kmFK = foreignKey("knesset_member_contact_fkey", kmId, TableClasses.knessetMembers)(_.id)
  def * = (kmId, platform, title, details, note) <> (ContactOption.tupled, ContactOption.unapply)
}

class PartyTable(tag:Tag) extends Table[Party](tag, "parties") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def webPage = column[String]("web_page")
  
  def * = (id, name, webPage) <> (Party.tupled, Party.unapply)
}

class ImageTable(tag:Tag) extends Table[KMImage](tag, "images") {
  def kmId = column[Long]("km_id", O.PrimaryKey)
  def suffix = column[String]("suffix")
  def mimeType = column[String]("mime_type")
  def date = column[Timestamp]("date")
  def credit = column[String]("credit")

  def kmFK = foreignKey("image_knesset_member_fkey", kmId, TableClasses.knessetMembers)(_.id)
  def * = (kmId, suffix, mimeType, date, credit) <> (KMImage.tupled, KMImage.unapply)
}

class GroupTable(tag:Tag) extends Table[KmGroupDN](tag, "groups") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  def * = (id, name) <> (KmGroupDN.tupled, KmGroupDN.unapply)
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
  def partyName = column[String]("party_name")
  def partyWebPage = column[String]("party_web_page")

  def * = (id, name, gender, isActive, webPage, partyId, partyName, partyWebPage) <> (
      { case (id, name, gender, isActive, webPage, partyId, partyName, partyWebPage) =>
                    KmsParties(KnessetMember(id, name, gender, isActive, webPage, partyId), Party(partyId, partyName, partyWebPage))},
      { kmp: KmsParties => Some(kmp.km.id, kmp.km.name, kmp.km.gender, kmp.km.isActive,
                    kmp.km.webPage, kmp.km.partyId, kmp.party.name, kmp.party.webPage)}
  )
}


object TableClasses {
  val parties = TableQuery[PartyTable]
  val knessetMembers = TableQuery[KnessetMemberTable]
  val groups = TableQuery[GroupTable]
}