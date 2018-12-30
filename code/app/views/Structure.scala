package views

import controllers.routes
import models.Campaign
import play.api.mvc.Call

/*
This file contains classes and data structures that describe the site structure (or structure*s*, in case
there are a few sections).
 */

abstract sealed class SectionItem
case class PageSectionItem(title:String, call:Call) extends SectionItem
case object SeparatorSectionItem extends SectionItem

abstract sealed class TopSiteSection[T]{
  def id:T
  def title:String
}

case class PageSection[T](title:String, id:T, call:Call) extends TopSiteSection[T]
case class MultiPageSection[T](title:String, id:T, children:Seq[SectionItem]) extends TopSiteSection[T]

object PublicSections extends Enumeration {
  val Home = Value("Home")
  val Login = Value("Login")
  val Components = Value("Components")
  val Others = Value("Others")
}

object BackOfficeSections extends Enumeration {
  val Home             = Value("Home")
  val Users            = Value("Users")
  val Knesset          = Value("Knesset")
  val ManageSystemCampaigns = Value("Campaigns")
  val MyCampaigns      = Value("MyCampaigns")
  val CampaignSettings = Value("CampaignSettings")
  val CampaignKnessetStatus   = Value("CampaignStatus")
  val CampaignAdmin    = Value("CampaignAdmin")
}

object CampaignEditorSections extends Enumeration {
  val Details         = Value("Details")
  val Settings        = Value("Settings")
  val Messages        = Value("Messages")
  val Design          = Value("Design")
  val FrontPage       = Value("FrontPage")
  val KnessetMembers  = Value("KnessetMembers")
}


/**
  * Holds data about the site structure.
  */
object Structure {
  
  val publicItems:Seq[TopSiteSection[PublicSections.Value]] = Seq(
    PageSection("Public Home", PublicSections.Home, routes.HomeCtrl.index),
    PageSection("Login", PublicSections.Login, routes.UserCtrl.showLogin)
  )
  
  val adminSiteSections:Seq[TopSiteSection[BackOfficeSections.Value]] = Seq(
    PageSection("navbar.main", BackOfficeSections.Home, routes.UserCtrl.userHome() ),
    MultiPageSection("navbar.knesset", BackOfficeSections.Knesset, Seq(
      PageSectionItem("navbar.parties", routes.KnessetMemberCtrl.showParties()),
      PageSectionItem("navbar.knessetMembers", routes.KnessetMemberCtrl.showKms(None, None, None)),
      PageSectionItem("navbar.groups", routes.KnessetMemberCtrl.showGroups())
    )),
    MultiPageSection("navbar.campaigns", BackOfficeSections.ManageSystemCampaigns, Seq(
      PageSectionItem("navbar.campaigns.list", routes.CampaignAdminCtrl.showCampaigns() ),
      PageSectionItem("navbar.campaigns.new", routes.CampaignAdminCtrl.createCampaign() )
    )),
    MultiPageSection("navbar.users.title", BackOfficeSections.Users, Seq(
      PageSectionItem("navbar.users.invite", routes.UserCtrl.showInviteUser()),
      PageSectionItem("navbar.users.list", routes.UserCtrl.showUserList(None))
    ))
  )
  
  val campaignManagerItems:Seq[TopSiteSection[BackOfficeSections.Value]] = Seq(
    PageSection("navbar.campaigner.myCampaigns", BackOfficeSections.MyCampaigns, routes.CampaignMgrCtrl.index()),
    MultiPageSection("navbar.campaigner.campaignAdmin", BackOfficeSections.CampaignAdmin, Seq(
      PageSectionItem("navbar.campaigner.campaignSettings.users", routes.HomeCtrl.notImplYet()),
      PageSectionItem("navbar.campaigner.campaignSettings.goLive", routes.HomeCtrl.notImplYet())
    )),
    MultiPageSection("navbar.campaigner.campaignSettings", BackOfficeSections.CampaignSettings, Seq(
      PageSectionItem("navbar.campaigner.campaignSettings.texts", routes.HomeCtrl.notImplYet()),
      PageSectionItem("navbar.campaigner.campaignSettings.webPage", routes.HomeCtrl.notImplYet())
    )),
    PageSection("navbar.campaigner.campaignKnessetStatus", BackOfficeSections.CampaignKnessetStatus, routes.HomeCtrl.notImplYet())
  )

  def campaignEditorItems(campaign:Campaign):Seq[TopSiteSection[CampaignEditorSections.Value]] = Seq(
    PageSection("navbar.campagins.mgmt.details",   CampaignEditorSections.Details, routes.CampaignMgrCtrl.details(campaign.id)),
    PageSection("navbar.campagins.mgmt.settings",  CampaignEditorSections.Settings, routes.CampaignMgrCtrl.settings(campaign.id)),
    PageSection("navbar.campagins.mgmt.messages",  CampaignEditorSections.Messages, routes.CampaignMgrCtrl.editMessages(campaign.id)),
    PageSection("navbar.campagins.mgmt.design",    CampaignEditorSections.Design, routes.HomeCtrl.notImplYet()),
    PageSection("navbar.campagins.mgmt.frontPage", CampaignEditorSections.FrontPage, routes.HomeCtrl.notImplYet()),
    PageSection("navbar.campagins.mgmt.positions", CampaignEditorSections.KnessetMembers, routes.CampaignMgrCtrl.positions(campaign.id))
  )


  
}
