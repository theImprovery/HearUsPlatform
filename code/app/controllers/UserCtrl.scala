package controllers

import java.sql.Timestamp
import java.util.UUID

import be.objectify.deadbolt.scala.{ActionBuilders, AuthenticatedRequest, DeadboltActions, allOfGroup}
import dataaccess._
import javax.inject.Inject
import models.{Campaign, CampaignFactory, CampaignStatus, Invitation, PasswordResetRequest, User, UserCampaign, UserRole}
import play.api.{Configuration, Logger, cache}
import play.api.cache.Cached
import play.api.data._
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.{Action, Call, ControllerComponents, InjectedController, Request, Result}
import security.{DeadboltHandler, HearUsSubject}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Success}


case class UserFormData( username:String,
                         name:String,
                         email:Option[String],
                         pass1:Option[String],
                         pass2:Option[String],
                         uuid:Option[String],
                         captchaAns:Option[String]) {
  def update(u:User) = u.copy(name=name, email=email.getOrElse(""))
}
object UserFormData {
  def of( u:User ) = UserFormData(u.username, u.name, Option(u.email), Option(""), Option(""), None, None)
}
case class LoginFormData( username:String, password:String )
case class ForgotPassFormData ( email:String )
case class ResetPassFormData ( password1:String, password2:String, uuid:String)
case class ChangePassFormData ( previousPassword:String, password1:String, password2:String)
case class RequestInviteFormData( email:String, answer:String )

/**
  * Controller for user-related actions (login, account mgmt...)
  * @param deadbolt
  * @param conf
  * @param cached
  * @param cc
  * @param users
  * @param invitations
  * @param forgotPasswords
  * @param mailerClient
  */
class UserCtrl @Inject()(deadbolt:DeadboltActions, conf:Configuration,
                         cached: Cached, cc:ControllerComponents,
                         users: UserDAO, invitations:InvitationDAO,
                         forgotPasswords:PasswordResetRequestDAO,
                         knesset: KnessetMemberDAO, campaigns:CampaignDAO,
                         committees: KmGroupDAO, usersCampaigns:UserCampaignDAO,
                         mailerClient: MailerClient, langs:Langs, messagesApi:MessagesApi) extends InjectedController {

  implicit private val ec: ExecutionContext = cc.executionContext
  implicit private val cfg:Configuration = conf
  private val logger = Logger(classOf[UserCtrl])
  
  private val validUserId = "^[-._a-zA-Z0-9]+$".r
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(langs.availables.head, messagesApi)
  }
  
  val userForm: Form[UserFormData] = Form(mapping(
      "username" -> text(minLength = 1, maxLength = 64)
        .verifying( "Illegal characters found. Use letters, numbers, and -_. only.", s=>validUserId.findFirstIn(s).isDefined),
      "name"     -> nonEmptyText,
      "email"    -> optional(email),
      "password1" -> optional(text),
      "password2" -> optional(text),
      "uuid"      -> optional(text),
      "answer"    -> optional(text)
    )(UserFormData.apply)(UserFormData.unapply)
  )

  val loginForm: Form[LoginFormData] = Form(mapping(
    "username" -> text,
    "password" -> text
    )(LoginFormData.apply)(LoginFormData.unapply)
  )

  private val emailForm = Form(mapping(
    "email" -> email
    )(ForgotPassFormData.apply)(ForgotPassFormData.unapply)
  )
  
  private val requestInviteForm = Form(mapping(
    "email" -> email,
    "answer" -> text
  )(RequestInviteFormData.apply)(RequestInviteFormData.unapply)
  )
  
  private val resetPassForm = Form(mapping(
    "password1" -> text,
    "password2" -> text,
    "uuid" -> text
    )(ResetPassFormData.apply)(ResetPassFormData.unapply)
  )

  private val changePassForm = Form(mapping(
    "previousPassword" -> text,
    "password1" -> text,
    "password2" -> text
    )(ChangePassFormData.apply)(ChangePassFormData.unapply)
  )
  
  private def subjectPresentOrElse( withU:(User, Request[_])=>Result, noU:Request[_]=>Result )(implicit req:Request[_]):Future[Result] = {
    req.session.get(DeadboltHandler.USER_ID_SESSION_KEY) match {
      case None => Future(noU(req))
      case Some(usrId) => users.get(usrId.toLong).map( {
        case None => noU(req).withNewSession
        case Some(u) => withU(u,req)
      })
    }
  }
  
  def showLogin = Action.async{ implicit req =>
      subjectPresentOrElse(
        (_, _) => Redirect( routes.UserCtrl.userHome() ),
        _ => Ok( views.html.users.login(loginForm) )
    )
  }
  
  def doLogin = Action.async { implicit request =>
    loginForm.bindFromRequest().fold(
      badForm   => Future(BadRequest(views.html.users.login(badForm))),
      loginData => {
        users.authenticate(loginData.username.trim, loginData.password.trim).map( {
          case Some(user) => {
            val redirectTo = request.session.get("targetUrl").getOrElse( routes.UserCtrl.userHome().url )
            Redirect(redirectTo).withNewSession.withSession((DeadboltHandler.USER_ID_SESSION_KEY,user.id.toString))
          }
          case None => BadRequest(views.html.users.login(loginForm.fill(loginData).withGlobalError("login.error.badUsernameEmailOrPassword")))
        })
      }
    )
  }

  def doLogout = Action { implicit req =>
    Redirect(routes.HomeCtrl.index()).withNewSession
      .flashing(FlashKeys.MESSAGE->Informational(InformationalLevel.Success,
                                                 messagesApi.preferred(req).messages("login.logoutMessage"), "").encoded)
  }

  def userHome = deadbolt.SubjectPresent()(){ implicit req =>
    val user = req.subject.get.asInstanceOf[HearUsSubject].user
    for {
      mkCount <- knesset.countKMs
      ptCount <- knesset.countParties
      comCount <- committees.countGroups()
      campaignCount <- campaigns.count
      camps <- usersCampaigns.getCampaignsForUser(user.id)
    } yield {
      Ok( views.html.users.userHome(
            user, mkCount, ptCount, comCount, campaignCount,
            camps._1.toSeq.sortBy(_.title), camps._2)
      )
    }
  }

  def apiAddUser = Action(parse.tolerantJson).async { req =>
    if ( req.connection.remoteAddress.isLoopbackAddress ) {
      val payload = req.body.asInstanceOf[JsObject]
      val username = payload("username").as[JsString].value
      val password = payload("password").as[JsString].value
      val email = payload("email").as[JsString].value
      val user = User(0, username, "", email, Set(UserRole.Admin), users.hashPassword(password))

      users.addUser(user).map(u => Ok("Added user " + u.username))

    } else {
      Future( Forbidden("Adding users via API is only available from localhost") )
    }
  }


  def showEditUserPage() = deadbolt.SubjectPresent()(){ implicit req =>
    val user = req.subject.get.asInstanceOf[HearUsSubject].user
    Future(
      Ok( views.html.users.userEditorBackEnd(userForm.fill(UserFormData.of(user)),
        routes.UserCtrl.doSaveUser(),
        isNew=false, isInvited=false))
    )
  }

  def doSaveUser() = deadbolt.SubjectPresent()(){ implicit req =>
    val user = req.subject.get.asInstanceOf[HearUsSubject].user
    userForm.bindFromRequest().fold(
      fwe => Future(BadRequest(views.html.users.userEditorBackEnd(fwe, routes.UserCtrl.doSaveUser(), isNew = false, false
                        )(new AuthenticatedRequest(req, None), messagesProvider, cfg))),
      fData => {
        for {
          userOpt <- users.get(user.id)
          userByEmail <- fData.email.map( eml => users.getUserByEmail(eml.trim) ).getOrElse( Future(None) )
          canUpdate = userByEmail.isEmpty || userOpt.forall(u => userByEmail.get.id == u.id)
          _ <- if (canUpdate) {
                  userOpt.map( user => users.updateUser(fData.update(user)) ).getOrElse(Future(()))
                } else Future(())
        } yield {
          userOpt match {
            case Some(_) => {
              if ( canUpdate ) {
                val message = Informational(InformationalLevel.Success,
                  Messages("userEditor.changesSaved"))
                Redirect(routes.UserCtrl.showEditUserPage()).flashing(FlashKeys.MESSAGE->message.encoded)
              } else {
                val form = userForm.fill(fData).withError("email","error.email.exists")
                Ok( views.html.users.userEditorBackEnd(form,
                  routes.UserCtrl.doSaveUser(),
                  isNew=false, isInvited=false))
              }
            }
            case None => notFound(user.id.toString)
          }
        }
      }
    )
  }

  def showNewUserPage = deadbolt.SubjectPresent()(){ implicit req =>
    Future(Ok( views.html.users.userEditorBackEnd(userForm, routes.UserCtrl.doSaveNewUser, true, true)(new AuthenticatedRequest(req, None), messagesProvider, cfg) ))
  }
  
  def doSaveNewUser = deadbolt.SubjectPresent()(){ implicit req =>
    userForm.bindFromRequest().fold(
      fwe => Future(BadRequest(views.html.users.userEditorBackEnd(fwe, routes.UserCtrl.doSaveNewUser, isNew=true, true)(
        new AuthenticatedRequest(req, None), messagesProvider, cfg))),
      fData => {
        processUserForm(fData, req.session.get("answer")).flatMap {
          case Right(b) => {
            val user = User(0, fData.username, fData.name, fData.email.getOrElse(""),
              Set(UserRole.Campaigner),
              users.hashPassword(fData.pass1.get.trim))
            users.tryAddUser(user).flatMap({
              case Success(user) => Future(Redirect(routes.UserCtrl.showUserList(None))//.withNewSession.withSession("userId" -> user.id.toString)
                .flashing(FlashKeys.MESSAGE -> Informational(InformationalLevel.Success, messagesProvider.messages("account.created")).encoded))
              case Failure(exp) => Future(BadRequest(views.html.users.userEditorBackEnd(
                userForm.fill(fData).withGlobalError(exp.getMessage), routes.UserCtrl.doSaveNewUser(), isNew = true, true
              )(new AuthenticatedRequest(req, None), messagesProvider, cfg)))
            })
          }
          case Left(form) => Future(BadRequest(views.html.users.userEditorBackEnd(form, routes.UserCtrl.doSaveNewUser(), isNew = true, true)(
            new AuthenticatedRequest(req, None), messagesProvider, cfg)))
        }
      }
    )
  }
  
  def showSignupPage = Action { implicit req =>
    val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
    val randomWord = Random.nextInt(randomSentence.split("\\s").length)
      Ok( views.html.users.userEditorBS( userForm, routes.UserCtrl.doSignup, isNew=true, false, randomSentence=randomSentence, randomWord=randomWord
                                    )(req, messagesProvider, cfg)).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord))
  }

  def showSignupPageForNewCampaign(title:String) = Action.async { implicit req =>
    subjectPresentOrElse(
      (_,_) => Redirect( routes.UserCtrl.userHome() ),
      (_) => {
        val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
        val randomWord = Random.nextInt(randomSentence.split("\\s").length)
          Ok( views.html.users.userEditorBS( userForm, routes.UserCtrl.doSignupForNewCampaign(title), isNew=true, false, randomSentence=randomSentence, randomWord=randomWord
                  )(req, messagesProvider, cfg)).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord))
      }
    )
  }
  
  def doSignup = Action.async{ implicit req =>
    userForm.bindFromRequest().fold(
      fwe => {
        logger.info(fwe.errors.toString)
        val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
        val randomWord = Random.nextInt(randomSentence.split("\\s").length)
        Future(BadRequest(views.html.users.userEditorBS(fwe, routes.UserCtrl.doSignup, isNew=true, false,
          randomSentence=randomSentence, randomWord=randomWord)(req,messagesProvider, cfg)).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord)))
      },
      fData => {
        val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
        val randomWord = Random.nextInt(randomSentence.split("\\s").length)
          processUserForm( fData, req.session.get("answer"), false).flatMap {
            case Right(b) => {
              val user = User(0, fData.username, fData.name, fData.email.getOrElse(""),
                Set(UserRole.Campaigner),
                users.hashPassword(fData.pass1.get.trim))
              users.tryAddUser(user).map({
                case Success(user) => Redirect(routes.UserCtrl.userHome()).withNewSession.withSession("userId" -> user.id.toString)
                  .flashing(FlashKeys.MESSAGE -> Informational(InformationalLevel.Success, messagesProvider.messages("account.created")).encoded)
                case Failure(exp) => BadRequest(views.html.users.userEditorBS(userForm.fill(fData).withGlobalError(exp.getMessage),
                  routes.UserCtrl.doSignup(), isNew = true, false, randomSentence=randomSentence, randomWord=randomWord)(req, messagesProvider, cfg))
                  .addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord))
              })
            }
            case Left(form) => Future(BadRequest(views.html.users.userEditorBS(form, routes.UserCtrl.doSignup(),
              isNew = true, false, randomSentence=randomSentence, randomWord=randomWord)(req, messagesProvider, cfg))
              .addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord)))
          }
      }
    )
  }

  def doSignupForNewCampaign(title:String) = Action.async{ implicit req =>
    val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
    val randomWord = Random.nextInt(randomSentence.split("\\s").length)
    userForm.bindFromRequest().fold(
    fwe => {
        logger.info(fwe.errors.toString)
        Future(BadRequest(views.html.users.userEditorBS(fwe, routes.UserCtrl.doSignupForNewCampaign(title),
          isNew=true, false, randomSentence=randomSentence, randomWord=randomWord)(req, messagesProvider, cfg))
          .addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord)))
      },
      fData => {
        processUserForm( fData, req.session.get("answer"), false).flatMap {
          case Right(b) => {
            val user = User(0, fData.username, fData.name, fData.email.getOrElse(""),
              Set(UserRole.Campaigner),
              users.hashPassword(fData.pass1.get.trim))
              users.tryAddUser(user).flatMap({
                case Success(user) => {
                  for {
                    campaign <- campaigns.store(CampaignFactory.createWithDefaults(title, conf.getOptional[String]("hearUs.defaultCampaignStyle").getOrElse("")))
                    rel <- usersCampaigns.connectUserToCampaign(UserCampaign(user.id, campaign.id, isAdmin = true))
                    _ <- campaigns.initializeCampaignPositions(campaign.id)
                  } yield {
                    Redirect(routes.CampaignMgrCtrl.details(campaign.id, true)).withNewSession.withSession("userId" ->user.id.toString)
                  }
                }
                case Failure(exp) => Future(BadRequest(
                  views.html.users.userEditorBS(userForm.fill(fData).withGlobalError(exp.getMessage),
                    routes.UserCtrl.doSignupForNewCampaign(title), isNew = true, false, randomSentence=randomSentence, randomWord=randomWord
                  )(req, messagesProvider, cfg)).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord)))
              })
          }
          case Left(form) => {
            Future(BadRequest(views.html.users.userEditorBS(form, routes.UserCtrl.doSignupForNewCampaign(title),
              isNew = true, false, randomSentence=randomSentence, randomWord=randomWord
            )(req, messagesProvider, cfg)).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord)))
          }
        }
      }
    )
  }
  
  private def showSignupForm( frm:Form[RequestInviteFormData])(implicit req:Request[_] ) = {
    val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
    val words = randomSentence.replace(",","").replace(".", "").split("\\s")
    val wordNum = Random.nextInt(words.length)
    
    Ok( views.html.users.requestInvite(frm, randomSentence, wordNum) ).addingToSession("answer"->words(wordNum))
  }
  
  def showRequestInvite() = Action { implicit req =>
    showSignupForm(requestInviteForm)
  }
  
  def doRequestInvite() = Action.async { implicit req =>
    requestInviteForm.bindFromRequest().fold(
      fwe => Future(showSignupForm(fwe)),
      reqInviteData => {
        for {
          emailExists <- users.emailExists( reqInviteData.email )
          answerOk = req.session("answer") == reqInviteData.answer
          invitationId = UUID.randomUUID.toString
          inviteOpt <- if (!emailExists && answerOk ) {
            invitations.add(Invitation(reqInviteData.email, new Timestamp(System.currentTimeMillis()), invitationId, "")).map(Some(_))
          } else Future(None)
          
        } yield inviteOpt match {
          case None => {
            var frm = requestInviteForm.fill(reqInviteData)
            if (emailExists) frm = frm.withError("email", "error.email.exists")
            if (!answerOk) frm = frm.withError("answer", "signup.wrongAnswer")
            showSignupForm(frm)
          }
          
          case Some(invite) => {
            sendInvitationEmail(invite)
            val message = Informational(InformationalLevel.Success,
              Messages("inviteEmail.confirmationMessage"),
              Messages("inviteEmail.confirmationDetails",reqInviteData.email))
            Redirect(routes.HomeCtrl.index()).flashing(FlashKeys.MESSAGE->message.encoded)
          }
        }
      }
    )
  }
  
  private def processUserForm(fData:UserFormData, answer:Option[String], isInvited:Boolean=true):Future[Either[Form[UserFormData], Boolean]] = {
    for {
      usernameExists <- users.usernameExists(fData.username)
      emailExists <- fData.email.map(users.emailExists).getOrElse(Future(false))
      passwordOK = fData.pass1.nonEmpty &&
        fData.pass1.map(_.trim.length).getOrElse(0) > 0 &&
        fData.pass1.map(_.trim) == fData.pass2.map(_.trim)
      captchaOK = isInvited || (fData.captchaAns == answer)
      canCreateUser = !usernameExists && !emailExists && passwordOK && captchaOK
      res =
        if (canCreateUser) Right(true)
        else {
          var form = userForm.fill(fData)
          if (emailExists) form = form.withError("email", "error.email.exists")
          if (usernameExists) form = form.withError("username", "error.username.exists")
          if (!passwordOK) form = form.withError("password1", "error.password")
            .withError("password2", "error.password")
          if (!captchaOK) form = form.withError("answer", "signup.wrongAnswer")
          Left(form)
        }
    } yield res
  }

  def showNewUserInvitation(uuid:String) = Action.async { implicit req =>
    for {
      invitationOpt <- invitations.get(uuid)
    } yield invitationOpt match {
      case None => NotFound(views.html.errorPage(404,"Invitation not found"))
      case Some(inv) => {
        val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
        val randomWord = Random.nextInt(randomSentence.split("\\s").length)
        Ok( views.html.users.userEditorBS( userForm.bind(Map("uuid"->uuid, "email"->inv.email)).discardingErrors,
          routes.UserCtrl.doNewUserInvitation,
          isNew=true, isInvited=true, randomSentence=randomSentence, randomWord=randomWord)(req, messagesProvider, cfg)
        ).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord))
      }
    }
  }

  def doNewUserInvitation() = Action.async { implicit req =>
    userForm.bindFromRequest().fold(
      fwe => {
        val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
        val randomWord = Random.nextInt(randomSentence.split("\\s").length)
        logger.info(fwe.data.mkString("\n"))
        Future(
          BadRequest(views.html.users.userEditorBS(fwe, routes.UserCtrl.doNewUserInvitation,
            isNew=true, isInvited=true, activeFirst=true, randomSentence, randomWord
          )(req, messagesProvider, cfg)
          ).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord)))
      },
      fData => {
        val res = for {
          uuidExists     <- fData.uuid.map(invitations.exists).getOrElse(Future(false))
          usernameExists <- users.usernameExists(fData.username)
          emailExists    <- fData.email.map(users.emailExists).getOrElse(Future(false))
          passwordOK     = fData.pass1.nonEmpty && fData.pass1 == fData.pass2
          captchaOK      = fData.captchaAns == req.session.get("answer")
          canCreateUser  = uuidExists && !usernameExists && !emailExists && passwordOK && captchaOK
        } yield {
          if (canCreateUser){
            val user = User(0, fData.username, fData.name, fData.email.getOrElse(""), Set(UserRole.Campaigner),
              users.hashPassword(fData.pass1.get))
            invitations.delete(fData.uuid.get)
            users.addUser(user).map(nUser => Redirect(routes.UserCtrl.userHome()).withNewSession.withSession("userId"->nUser.id.toString))

          }
          else{
            var form = userForm.fill(fData)
            if ( !uuidExists ) form = form.withError("uuid", "error.invitation.doesNotExist")
            if ( usernameExists ) form = form.withError("username", "error.username.exists")
            if ( emailExists ) form = form.withError("email", "error.email.exists")
            if ( !passwordOK ) form = form.withError("password1", "error.password")
              .withError("password2", "error.password")
            if( !captchaOK ) form = form.withError("answer", "signup.wrongAnswer")
            val randomSentence = Messages("signup.sentence." + Random.nextInt(10))
            val randomWord = Random.nextInt(randomSentence.split("\\s").length)
            Future(BadRequest(views.html.users.userEditorBS(form, routes.UserCtrl.doNewUserInvitation, isNew = true, false, randomSentence=randomSentence, randomWord=randomWord
                                                          )(req, messagesProvider, cfg)).addingToSession("answer"->randomSentence.replace(",","").replace(".", "").split("\\s")(randomWord)))
          }
        }
        
        scala.concurrent.Await.result(res, Duration(2000, scala.concurrent.duration.MILLISECONDS))

      }
    )

  }

  def showUserList(search:Option[String]) = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(){ implicit req =>
    val effectiveSearch = search.map(_.trim).flatMap(v => if (v.isEmpty) None else Some(v))
    val sqlSearch = search.map(s => "%" + s.trim + "%")
    val user = req.subject.get.asInstanceOf[HearUsSubject].user
    users.allUsers(sqlSearch).map( users => Ok(views.html.users.userList(users, user, effectiveSearch)) )
  }
  
  private def notFound(userId:String) = NotFound("User with username '%s' does not exist.".format(userId))

  def showForgotPassword = Action { implicit req =>
    Ok( views.html.users.forgotPassword(None,None) )
  }

  def doForgotPassword = Action.async{ implicit req =>
    emailForm.bindFromRequest().fold(
      fwi => Future(BadRequest(views.html.users.forgotPassword(None,Some(Messages("forgotPassword.formProcessError"))))),
      fd => {
        for {
          userOpt <- users.getUserByEmail(fd.email)
          userSessionId = UUID.randomUUID.toString
          emailExists <- userOpt.map(u => forgotPasswords.add(
            PasswordResetRequest(u.username, userSessionId, new Timestamp(System.currentTimeMillis())))
            .map(_=>true))
            .getOrElse(Future(false))
        } yield {
          if ( emailExists ){
            val bodyText = "To reset your password, please click the link below: \n " + conf.get[String]("psps.server.publicUrl") +
              routes.UserCtrl.showResetPassword(userSessionId).url
            val email = Email("Forgot my password", conf.get[String]("hearUs.mailerUserName"), Seq(fd.email), bodyText = Some(bodyText))
            mailerClient.send(email)
            val msg = Informational( InformationalLevel.Success, Messages("forgotPassword.emailSent", fd.email), "")
            Redirect( routes.UserCtrl.showLogin() ).flashing( FlashKeys.MESSAGE->msg.encoded )
          }
          else {
            BadRequest(views.html.users.forgotPassword(Some(fd.email), Some(Messages("forgotPassword.emailNotFound"))))
          }
        }
      }
    )
  }

  def showResetPassword(requestId:String) = Action.async { implicit req =>
    forgotPasswords.get(requestId).map( {
      case None => NotFound( views.html.errorPage(404, Messages("passwordReset.requestNotFound"), None, None) )
      case Some(prr) => {
        if ( isRequestExpired(prr) ) {
          forgotPasswords.deleteForUser(requestId)
          Gone(views.html.errorPage(410, Messages("passwordReset.requestExpired"), None, None))
        } else {
          Ok(views.html.users.passwordReset(prr, None))
        }
      }
    })
    
  }

  def doResetPassword() = Action.async{ implicit req =>
    resetPassForm.bindFromRequest().fold(
      fwi => Future(BadRequest(views.html.users.passwordReset(PasswordResetRequest("","",null), Some("Error processing reset password form")))),
      fd => {
        for {
          prrOpt      <- forgotPasswords.get(fd.uuid)
          userOpt     <- prrOpt.map(u => users.get(u.username)).getOrElse(Future(None))
          invalidPass =  fd.password1.trim.isEmpty || fd.password1 != fd.password2
          reqExpired  =  prrOpt.exists(u => isRequestExpired(u))
          
        } yield {
          prrOpt match {
            case None => NotFound( views.html.errorPage(404, Messages("passwordReset.requestNotFound"), None, None))
            case Some(prr) => {
              if ( invalidPass ) BadRequest(views.html.users.passwordReset(prr, Some(Messages("passwordReset.validationFailed"))))
              else if (reqExpired ) Gone(views.html.errorPage(410, Messages("passwordReset.requestExpired"), None, None))
              else {
                userOpt match {
                  case None => {
                    // user might have been deleted
                    Gone(views.html.errorPage(410, Messages("passwordReset.requestExpired"), None, None))
                  }
                  case Some(user) => {
                    // we're OK to reset
                    forgotPasswords.deleteForUser(prr.username)
                    users.updatePassword(user, fd.password1)
                    Redirect(routes.UserCtrl.userHome()).withNewSession.withSession(("userId", user.id.toString)).flashing(FlashKeys.MESSAGE->Messages("passwordReset.success"))
                  }
                }
              }
            }
          }
        }
      }
    )
  }

  def showInviteUser = deadbolt.SubjectPresent()(){ implicit req =>
    val user = req.subject.get.asInstanceOf[HearUsSubject].user
    for {
      invitations <- if (user.roles.contains(UserRole.Admin)) invitations.all else Future(Seq())
    } yield Ok(views.html.users.inviteUser(invitations, user.roles.contains(UserRole.Admin)))
  }

  def doInviteUser = deadbolt.SubjectPresent()(){ implicit req =>
    val user = req.subject.get.asInstanceOf[HearUsSubject].user
    emailForm.bindFromRequest().fold(
      formWithErrors => {
        logger.info( formWithErrors.errors.mkString("\n") )
        for {
          invitations <- invitations.all
        } yield BadRequest(views.html.users.inviteUser(invitations))
      },
      fd => {
        val invitationId = UUID.randomUUID.toString
        invitations.add(Invitation(fd.email, new Timestamp(System.currentTimeMillis()), invitationId, user.email)).map( invite =>{
          sendInvitationEmail(invite)
          val message = Informational(InformationalLevel.Success,
                                      Messages("inviteEmail.confirmationMessage"),
                                      Messages("inviteEmail.confirmationDetails",fd.email))
          Redirect(routes.UserCtrl.userHome()).flashing(FlashKeys.MESSAGE->message.encoded)
        })
      }
    )
  }
  
  def apiReInviteUser(invitationUuid:String) = deadbolt.SubjectPresent()(){ implicit req =>
    for {
      invitationOpt <- invitations.get( invitationUuid )
    } yield {
      invitationOpt match {
        case None => NotFound
        case Some(invitation) => sendInvitationEmail(invitation); Ok
      }
    }
  }
  
  def apiDeleteInvitation(invitationUuid:String) = deadbolt.SubjectPresent()() { implicit req =>
    invitations.delete(invitationUuid).map( _ => Ok )
  }
  
  def sendInvitationEmail( invi:Invitation ): Unit = {
    val link = conf.get[String]("psps.server.publicUrl") + routes.UserCtrl.showNewUserInvitation(invi.uuid).url
    val bodyText = Messages("inviteEmail.body", link)
    val email = Email(Messages("inviteEmail.title"), conf.get[String]("hearUs.mailerUserName"), Seq(invi.email), Some(bodyText))
    mailerClient.send(email)
    invitations.updateLastSend( invi.uuid, java.time.LocalDateTime.now() )
  }
  
  def doChangePassword = deadbolt.SubjectPresent()(){ implicit req =>
    val user = req.subject.get.asInstanceOf[HearUsSubject].user
    changePassForm.bindFromRequest().fold(
      fwi => {
        Future(BadRequest(views.html.users.userEditorBackEnd(userForm, routes.UserCtrl.doSaveNewUser, isNew = false, false)))
      },
      fd => {
        if(users.verifyPassword(user, fd.previousPassword)){
          if (fd.password1.nonEmpty && fd.password1 == fd.password2) {
            users.updatePassword(user, fd.password1).map(_ => {
              val message = Informational(InformationalLevel.Success, Messages("password.changed"))
              Redirect(routes.UserCtrl.userHome()).withNewSession.withSession(("userId",user.id.toString)).flashing(FlashKeys.MESSAGE->message.encoded)
            })
          } else {
            val form = userForm.fill(UserFormData of user).withError("password1", "error.password")
              .withError("password2", "error.password")
            Future(BadRequest(views.html.users.userEditorBackEnd(form, routes.UserCtrl.doSaveNewUser, isNew = false, false, activeFirst=false)))
          }
        } else{
          val form = userForm.fill(UserFormData of user).withError("previousPassword", "error.password.incorrect")
          Future(BadRequest( views.html.users.userEditorBackEnd(form, routes.UserCtrl.doSaveNewUser, isNew=false, false, activeFirst=false )))
        }
      }
    )
  }
  
  def isRequestExpired( prr:PasswordResetRequest ):Boolean = {
    val oneWeek = 1000 * 60 * 60 * 24 * 7
    val currentTime = System.currentTimeMillis()
    (currentTime - prr.resetPasswordDate.getTime) > oneWeek
  }

  def updateRole() = deadbolt.Restrict(allOfGroup(UserRole.Admin.toString))(cc.parsers.tolerantJson) { implicit req =>
    logger.info("update role")
    val json = req.body.as[JsObject]
    val roles = if(json("isAdmin").asOpt[Boolean].getOrElse(true)) { Set(UserRole.Admin, UserRole.Campaigner) }
            else { Set(UserRole.Campaigner) }
    users.updateRole(json("id").asOpt[Long].getOrElse(-1L), roles).map(ans => Ok("updated"))
  }
  
}
