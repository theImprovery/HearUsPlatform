package models

case class KmGroups(id:Long,
                    name:String,
                    kms:Set[Long] )

case class KmGroupDN(id:Long,
                     name:String)

case class GroupIdKmId(groupId:Long, kmId:Long)