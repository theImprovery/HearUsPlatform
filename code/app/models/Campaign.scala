package models

import java.sql.Timestamp

object Gender extends Enumeration {
  type Gender = Value
  val Female:Gender = Value("female")
  val Male:Gender = Value("male")
}


object Position extends Enumeration {
  type Position = Value
  val Against, Undecided, Neutral, For = Value
}

object ActionType extends Enumeration {
  type ActionType = Value
  val Interview, Vote, OfficialPosition, Post, EMail = Value
  
}

case class Campaign( id:Long,
                     title:  String,
                     slogan: String,
                     slug:   Option[String],
                     website:      String,
                     themeData:    String,
                     contactEmail: String,
                     analytics:    String,
                     status:  CampaignStatus.Value)


object CampaignFactory {
  
  private val letters = Range('A','Z')++Range('a','z')
  
  def makeSlug: String = {
    var cur = java.lang.System.currentTimeMillis()
    var out = collection.mutable.Buffer[Char]()
    while ( cur > 0 ){
      out += letters((cur % 32).toInt).toChar
      cur = cur >> 5
    }
    out.mkString
  }
  
  
  def createWithDefaults(title:String, design:String) = Campaign(-1L, title, "", Some(makeSlug), "", design, "", "", CampaignStatus.WorkInProgress )
}

/** Subset of `Campaign`, when there's no need to transfer
  * the entire object over app boundaries/network.
  *
  * @param title
  * @param slogan
  * @param website
  * @param contactEmail
  * @param analyticsCode
  */
case class CampaignDetails( title:  String,
                            slogan: String,
                            website:      String,
                            contactEmail: String,
                            analyticsCode: String )

case class CampaignText( campaignId: Long,
                         title:String,
                         subtitle:String,
                         bodyText:String,
                         footer:String,
                         groupLabels:String,
                         kmLabels:String) {
  val groupLabel:Map[Position.Value, String] = Position.values.toSeq.zipAll( groupLabels.split("\t").take(Position.values.size).map(_.trim), Position.For, "" ).toMap
  val kmLabel:Map[(Gender.Value,Position.Value),String] = Gender.values.toSeq.flatMap( g => Position.values.toSeq.map(p=>(g,p)) )
                                                                .zipAll(kmLabels.split("\t").map(_.trim)
                                                                                .take(Gender.values.size*Position.values.size),
                                                                        (Gender.Female, Position.For), ""
                                                                ).toMap
}

case class LabelText( camId: Long,
                      position: Position.Value,
                      gender: String,
                      text: String )

case class RelevantGroup( camId: Long, groupId: Long)

case class CannedMessage( camId: Long,
                          position: Position.Value,
                          gender: String,
                          platform: Platform.Value,
                          text: String ){
  
  def process(km: KnessetMember, twitterHandle:Option[String]) = {
    val updatedText = text.replaceAll("@name", km.name)
    val effTwitterHdl = twitterHandle.map(t => t.trim).map( t=> if (t.startsWith("@")) t else "@"+t )
    copy( text=updatedText.replaceAll("@twitter", effTwitterHdl.getOrElse(km.name)))
  }
}

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


case class UserCampaign( userId:Long,
                         campaignId:Long,
                         isAdmin:Boolean )


class CampaignTeam(val users:Set[User], campaignRelation:Iterable[UserCampaign] ) {
  private val adminIds = campaignRelation.filter(_.isAdmin).map(_.userId).toSet
  
  def isAdmin( aUser:User ) = adminIds(aUser.id)
}

object CampaignStatus extends Enumeration {
  type CampaignStatus = Value
  val WorkInProgress, PublicationRequested, Published, Rejected = Value
}