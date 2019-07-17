package models

case class KnessetMember (id:Long,
                          name:String,
                          gender:String,
                          isActive:Boolean,
                          webPage:String,
                          partyId:Long,
                          knessetKey:Long) {
  val genderVal = Gender.withName(gender.toLowerCase)
}

case class ContactOption (id:Long,
                          kmId:Option[Long],
                          campaignId:Option[Long],
                          platform:String,
                          title:String,
                          details:String,
                          note:String) {
  val platformObj = Platform.tryConvert(platform)
}

case class KmsParties(km:KnessetMember,
                      party:Party)