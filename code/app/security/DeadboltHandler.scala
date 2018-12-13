package security

import be.objectify.deadbolt.scala.AuthenticatedRequest
import be.objectify.deadbolt.scala.models.{Permission, Role, Subject}
import controllers.{FlashKeys, Informational, InformationalLevel, routes}
import dataaccess.UsersDAO
import models.User
import play.api.i18n._
import play.api.mvc.{Request, Result, Results}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class HearUsRole(name:String) extends Role

case class HearUsSubject(user:User) extends Subject {
  override def identifier: String = user.username
  override def roles:List[HearUsRole] = user.roles.map( r => HearUsRole(r.toString) ).toList
  override def permissions:List[Permission] = Nil
}


class DeadboltHandler(users:UsersDAO, langs:Langs, messagesApi:MessagesApi) extends be.objectify.deadbolt.scala.DeadboltHandler {
  
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  
  override def beforeAuthCheck[A](request: Request[A]) = Future(None)

  override def getDynamicResourceHandler[A](request: Request[A]) = Future(None)

  override def getSubject[A](request: AuthenticatedRequest[A]):Future[Option[Subject]] = {
    request.session.get("userId")
           .map( sId => users.get(sId.toLong).map(_.map(u=>HearUsSubject(u))) )
           .getOrElse( Future(None) )
  }

  /**
    * When authentication fails, store the target URL in the session and go to login page.
    * @param request the unauthorized request
    * @tparam A type of A
    * @return redirect to login page response
    */
  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    Future {
      val message = Informational(InformationalLevel.Warning, Messages("login.pleaseLogIn"))
      Results.Redirect(routes.UserCtrl.showLogin()).withSession(
        request.session + ("targetUrl" -> request.path)).flashing((FlashKeys.MESSAGE,message.encoded))
    }
  }


}