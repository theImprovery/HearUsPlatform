package models

import java.sql.Timestamp

case class SystemEvent(id:Long,
                       userId:Long,
                       date:Timestamp,
                       message:String,
                       details:String)
