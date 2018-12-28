package models

import java.sql.Timestamp
import dataaccess.Platform

case class Campaign( id:Long,
                     title: String,
                     subtitle: String,
                     website: String,
                     themeData: String,
                     contactEmail: String,
                     isPublished: Boolean)

case class LabelText( camId: Long,
                      position: Position.Value,
                      gender: String,
                      text: String )

case class RelevantGroup( camId: Long, groupId: Long)

case class CannedMessage( camId: Long,
                          position: Position.Value,
                          gender: String,
                          platform: Platform.Value,
                          text: String )

case class KmPosition(kmId: Long,
                      camId: Long,
                      position: Position.Value )

case class SocialMedia(id: Long,
                       camId: Long,
                       name: String,
                       service: String )

case class KmAction(id: Long,
                    camId: Long,
                    kmId: Long,
                    actionType: ActionType.Value,
                    date: Timestamp,
                    title: String,
                    details: String,
                    link: String )



object Position extends Enumeration {
  type Position = Value
  val Against, Undecided, Neutral, For = Value
}

object ActionType extends Enumeration {
  type ActionType = Value
  val Interview = Value
}