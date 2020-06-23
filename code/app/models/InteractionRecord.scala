package models

import java.time.LocalDateTime

case class InteractionRecord( id         :Int,
                              campaignId :Int,
                              kmId       :Int,
                              medium     :String,
                              link       :String,
                              time       :Option[LocalDateTime] )