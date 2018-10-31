package dataaccess

import java.sql.Timestamp
import java.text.SimpleDateFormat

import models.{ContactOption, KMImage, Party}
import play.api.libs.json._

object JSONFormats {

  implicit val JsonMapWrites:OWrites[Map[String,JsValue]] = new OWrites[Map[String,JsValue]] {
    override def writes(o: Map[String, JsValue]) =
      o.toSeq.filter(_._2 != null).foldLeft(Json.obj())((obj, pair) => obj + pair)
  }

  implicit val timestampFormat:Format[Timestamp] = new Format[Timestamp] {
    private val formats = new java.lang.ThreadLocal[SimpleDateFormat] {
      override def initialValue(): SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    }
    override def writes(o: Timestamp): JsValue = JsString(formats.get().format(o))

    override def reads(json: JsValue): JsResult[Timestamp] = {
      json match {
        case JsString(value) => JsSuccess(new Timestamp(formats.get().parse(value).getTime))
        case _ => JsError("Expecting a string with format yyyy-MM-dd hh:mm ")
      }
    }
  }

  implicit val fileFormat:Format[KMImage] = Json.format[KMImage]
  implicit val contactOptionFormat:Format[ContactOption] = Json.format[ContactOption]
  implicit val partyFormat:Format[Party] = Json.format[Party]
}
