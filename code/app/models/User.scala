package models

object UserRole extends Enumeration {
  val Admin = Value
  val Campaigner = Value
}

case class User(id:Long,
                username:String,
                name:String,
                email:String,
                roles:Set[UserRole.Value],
                encryptedPassword:String)