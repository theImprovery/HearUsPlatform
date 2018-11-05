package models
import java.sql.Timestamp

case class KMImage (kmId:Long,
                    suffix:String,
                    mimeType:String,
                    date:Timestamp,
                    credit:String) {
  def filename = s"$kmId.$suffix"
}