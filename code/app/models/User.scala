package models

import java.time.LocalDateTime

object UserRole extends Enumeration {
  val Admin = Value
  val Campaigner = Value
}

case class User(id:Long,
                username:String,
                name:String,
                email:String,
                roles:Set[UserRole.Value],
                encryptedPassword:String) {
  def dn = UserDN(id, username, email, name )
}

case class UserDN( id:Long, username:String, email:String, name:String )

case class EmailChange( userId: Long,
                        previousAddress: Option[String],
                        newAddress: Option[String],
                        changeDate: LocalDateTime
)