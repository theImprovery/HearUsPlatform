package controllers

import javax.inject._
import play.api._
import play.api.cache.Cached
import play.api.i18n.{I18nSupport, Langs, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import views.PaginationInfo

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */

class HomeCtrl @Inject()(langs: Langs, messagesApi: MessagesApi, cached: Cached, cc: ControllerComponents
                        ) extends AbstractController(cc) with I18nSupport {
  
//  implicit val mApiImplicit = messagesApi
  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.publicIndex())
  }
  
  def sampleNavbar = Action { implicit req =>
    Ok(views.html.sampleNavbar("Parametrized Message"))
  }
  
  def apiSayHi = Action(cc.parsers.tolerantJson) { implicit req =>
    val json = req.body.as[JsObject]
    val name = json("name")
    Ok(Json.obj("message"->s"Hello, $name."))
  }
  
  def informationals = Action{ implicit req =>
    Ok( views.html.informationalsSample() )
  }

  def styledInputs = Action{ implicit req =>
    Ok( views.html.styledInputsSample() )
  }
  
  /**
    * Routes for the front-end.
    * @return
    */
  def frontEndRoutes = cached("ACT_frontEndRoutes") {
    Action { implicit request =>
      Ok(
        routing.JavaScriptReverseRouter("feRoutes")(
          routes.javascript.HomeCtrl.apiSayHi
        )).as("text/javascript")
    }
  }
  
  /**
    * Routes for the back-end.
    * @return
    */
  def backEndRoutes = cached("ACT_backEndRoutes") {
    Action { implicit request =>
      Ok(
        routing.JavaScriptReverseRouter("beRoutes")(
          routes.javascript.Assets.versioned,
          routes.javascript.UserCtrl.apiAddUser,
          routes.javascript.UserCtrl.apiReInviteUser,
          routes.javascript.UserCtrl.apiDeleteInvitation,
          routes.javascript.FilesCtrl.apiAddFile,
//          routes.javascript.FilesCtrl.apiUpdateCredit,
//          routes.javascript.FilesCtrl.deleteFile,
          routes.javascript.FilesCtrl.apiFilesForKm,
          routes.javascript.FilesCtrl.apiGetImage,
          routes.javascript.KnessetMemberCtrl.showEditKM,
          routes.javascript.KnessetMemberCtrl.doEditKM,
          routes.javascript.KnessetMemberCtrl.deleteKM,
          routes.javascript.KnessetMemberCtrl.getContactOptionForKm,
          routes.javascript.KnessetMemberCtrl.updateContactOption,
          routes.javascript.KnessetMemberCtrl.updateParty,
          routes.javascript.KnessetMemberCtrl.deleteParty,
          routes.javascript.KnessetMemberCtrl.showKms,
          routes.javascript.KnessetMemberCtrl.showGroups,
          routes.javascript.KnessetMemberCtrl.showEditGroup,
          routes.javascript.KnessetMemberCtrl.showNewGroup,
          routes.javascript.KnessetMemberCtrl.doEditGroup,
          routes.javascript.KnessetMemberCtrl.deleteGroup,
          routes.javascript.ParseCtrl.apiKms,
          routes.javascript.ParseCtrl.apiUpdateCommittees,
          routes.javascript.CampaignCtrl.showCampaigns,
          routes.javascript.CampaignCtrl.editCampaign,
          routes.javascript.CampaignCtrl.createCampaign,
          routes.javascript.CampaignCtrl.updateDetails,
          routes.javascript.CampaignCtrl.deleteCampaign,
          routes.javascript.CampaignCtrl.getLabelText,
          routes.javascript.CampaignCtrl.updateLabels,
          routes.javascript.CampaignCtrl.getMessages,
          routes.javascript.CampaignCtrl.updateMessages,
          routes.javascript.CampaignCtrl.updatePosition,
          routes.javascript.CampaignCtrl.getSocialMedia,
          routes.javascript.CampaignCtrl.updateSocialMedia
        )).as("text/javascript")
    }
  }
  
  def notImplYet = TODO
  
}
