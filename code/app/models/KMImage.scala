package models
import java.sql.Timestamp

case class KMImage (id:Long,
                    kmId:Long,
                    suffix:String,
                    mimeType:String,
                    date:Timestamp,
                    credit:String)