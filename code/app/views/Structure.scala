package views

import controllers.routes
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
  val Home = Value("Home")
  val Users = Value("Users")
  val Knesset = Value("Knesset")
  val Campaigns = Value("Campaigns")
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
    MultiPageSection("navbar.campaigns", BackOfficeSections.Campaigns, Seq(
      PageSectionItem("navbar.campaigns.list", routes.HomeCtrl.notImplYet() ),
      PageSectionItem("navbar.campaigns.new", routes.HomeCtrl.notImplYet() )
    )),
    MultiPageSection("navbar.users.title", BackOfficeSections.Campaigns, Seq(
      PageSectionItem("navbar.users.invite", routes.UserCtrl.showInviteUser()),
      PageSectionItem("navbar.users.list", routes.UserCtrl.showUserList(None))
    ))
  )
  
}
