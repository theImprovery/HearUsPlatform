package models

object Platform extends Enumeration {
  type Platform = Value
  val Phone, WhatsApp, PhoneAndWhatsApp, Email, Mail, Fax, Facebook, Twitter, Instagram = Value
  
  def tryConvert(name:String):Option[Platform.Value] = {
    try {
      Some(Platform.withName(name))
    } catch {
      case e: Exception => None
    }
  }
}
