package dataaccess

import java.sql.Timestamp
import java.text.SimpleDateFormat

import models._
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

  implicit val positionFormat:Format[Position.Value ] = new Format[Position.Value ] {
    val writeFmt = Writes.enumNameWrites[Position.type]
    val readFmt = Reads.enumNameReads(Position)
    override def writes(o: Position.Value): JsValue = writeFmt.writes(o)
    override def reads(json: JsValue): JsResult[Position.Value] = readFmt.reads(json)
  }

  implicit val platformFormat:Format[Platform.Value ] = new Format[Platform.Value ] {
    val writeFmt = Writes.enumNameWrites[Platform.type]
    val readFmt = Reads.enumNameReads(Platform)
    override def writes(o: Platform.Value): JsValue = writeFmt.writes(o)
    override def reads(json: JsValue): JsResult[Platform.Value] = readFmt.reads(json)
  }

  implicit val rolesFormat:Format[UserRole.Value ] = new Format[UserRole.Value ] {
    val writeFmt = Writes.enumNameWrites[UserRole.type]
    val readFmt = Reads.enumNameReads(UserRole)
    override def writes(o: UserRole.Value): JsValue = writeFmt.writes(o)
    override def reads(json: JsValue): JsResult[UserRole.Value] = readFmt.reads(json)
  }

  implicit val fileFormat:Format[KMImage] = Json.format[KMImage]
  implicit val contactOptionFormat:Format[ContactOption] = Json.format[ContactOption]
  implicit val partyFormat:Format[Party] = Json.format[Party]
  implicit val campaignFormat:Format[Campaign] = Json.format[Campaign]
  implicit val lblFormat:Format[LabelText] = Json.format[LabelText]
  implicit val msgFormat:Format[CannedMessage] = Json.format[CannedMessage]
  implicit val smFormat:Format[SocialMedia] = Json.format[SocialMedia]
  implicit val kmPositionFormat:Format[KmPosition] = Json.format[KmPosition]
  implicit val userFormat:Format[User] = Json.format[User]
  implicit val userDnFormat:Format[UserDN] = Json.format[UserDN]
  implicit val campaignDetailsFormat:Format[CampaignDetails] = Json.format[CampaignDetails]
}
