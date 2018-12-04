package models
import java.sql.Timestamp

case class KMImage (id:Long,
                    kmId:Option[Long],
                    camId:Option[Long],
                    suffix:String,
                    mimeType:String,
                    date:Timestamp,
                    credit:String) {
  def filename = if(kmId.isDefined) s"${kmId.get}.$suffix" else s"${camId.get}.$suffix"
}

/*
case class FileDN (id:Long,
                   relatedType: String,
                   relatedId:   Long,
                   suffix:      String,
                   mimeType:    String,
                   date:        Timestamp,
                   caption:     Map[String,String])

object FileDN {
  def apply(file: TJFile) = {
    val related = (file.siteId, file.routeId, file.waypointId) match {
      case (Some(id), _, _) => ("site", id)
      case (_, Some(id), _) => ("route", id)
      case (_, _, Some(id)) => ("waypoint", id)
      case _ => throw new IllegalArgumentException("File does not have a related object id.")
    }
    new FileDN(file.id, related._1, related._2, file.suffix, file.mimeType, file.date,
      Map("en" -> file.caption, "ar" -> file.caption_ar, "he" -> file.caption_he))
  }
}
 */