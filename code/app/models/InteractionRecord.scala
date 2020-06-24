package models

import java.time.LocalDateTime

case class InteractionRecord( id         :Long,
                              campaignId :Long,
                              kmId       :Long,
                              medium     :String,
                              link       :String,
                              time       :Option[LocalDateTime] )

case class InteractionSummary(
                             campaignId :Long,
                             medium     :String,
                             count      :Long
                             )

case class InteractionDetails (
                                campaignId :Long,
                                time       :LocalDateTime,
                                medium     :String,
                                kmId       :Long,
                                kmName     :String,
                                partyId    :Long,
                                partyName  :String
                              )