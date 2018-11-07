package models

case class KnessetMember (id:Long,
                          name:String,
                          gender:String,
                          isActive:Boolean,
                          webPage:String,
                          partyId:Long)

case class ContactOption (kmId:Long,
                          platform:String,
                          title:String,
                          details:String,
                          note:String)

case class KmsParties(km:KnessetMember,
                      party:Party)