package actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class BindActors extends AbstractModule with AkkaGuiceSupport {
  override def configure():Unit = {
    bindActor[ImportCoordinationActor]("import-actor")
    bindActor[ImportCommitteesActor]("committee-actor")
    Range(0,ImportSinglePageActor.count).map( ImportSinglePageActor.nameOf )
      .foreach( bindActor[ImportSinglePageActor](_) )
    
  }
}
