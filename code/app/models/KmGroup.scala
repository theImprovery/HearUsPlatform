package models

case class KmGroups(id:Long,
                    name:String,
                    knessetKey:Long,
                    kms:Set[Long] )

case class KmGroupDN(id:Long,
                     name:String,
                     knessetKey:Long)

case class GroupIdKmId(groupId:Long, kmId:Long)