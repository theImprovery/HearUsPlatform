package actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class BindActors extends AbstractModule with AkkaGuiceSupport {
  override def configure():Unit = {
    bindActor[EmailSendingActor]("email-actor")
    bindActor[ImportCoordinationActor]("import-actor")
    bindActor[ImportCommitteesActor]("committee-actor")
    bindActor[InvalidateCacheActor]("cacheInvalidator")
    Range(0,ImportSinglePageActor.count).map( ImportSinglePageActor.nameOf )
      .foreach( bindActor[ImportSinglePageActor](_) )
    
  }
}
