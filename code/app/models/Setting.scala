package models

object SettingKey extends Enumeration {
  val HOME_PAGE_TEXT = Value
}

/**
  * Stores a setting in the database
  */
case class Setting(key:SettingKey.Value, value:String) {
  def isTrue = (value!=null) && Setting.truishValues.contains(value.toLowerCase)
}

object Setting {
  private val truishValues = Set("yes","1","true","ok")
  def isTruish(s:String) = truishValues(s)
}

