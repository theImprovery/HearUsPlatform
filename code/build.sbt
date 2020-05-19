name := """hear-us"""

organization := "il.org.drm"

version := "1.0-SNAPSHOT"

maintainer := "michael@codeworth.io"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  ehcache,
  ws,
  guice,
  "org.webjars" % "jquery" % "3.4.1",
  "org.webjars" % "jquery-ui" % "1.12.1",
//  "org.webjars" % "tether" % "1.4.0",
  "org.webjars" % "summernote" % "0.8.10",
  "org.webjars" % "popper.js" % "1.15.0",
  "org.webjars.bower" % "fontawesome" % "4.7.0",
  "be.objectify" %% "deadbolt-scala" % "2.7.1",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.postgresql" % "postgresql" % "42.2.11",
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  "org.seleniumhq.selenium" % "selenium-java" % "2.35.0" % "test",
  "com.typesafe.play" %% "play-mailer" % "8.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "8.0.0",
  "org.scala-lang.modules" %% "scala-xml" % "2.0.0-M1",
  "org.webjars" % "d3js" % "5.9.1",
  "org.webjars.bower" % "polyglot" % "2.2.2",
  "org.webjars" % "sweetalert" % "2.1.0",
  "org.webjars" % "requirejs" % "2.3.6",
  "org.webjars" % "bootstrap" % "4.4.1-1",
  "org.webjars.bower" % "hopscotch" % "0.3.1",
  "org.scalamock" %% "scalamock" % "4.4.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)

// TODO add sections and table helpers
// TwirlKeys.templateImports ++= Seq( "views.Sections", "views.TableHelper")

LessKeys.compress in Assets := true

includeFilter in (Assets, LessKeys.less) := "*.less"

LessKeys.compress in Assets := true

RjsKeys.modules += WebJs.JS.Object("esversion"->"6")

//pipelineStages := Seq(rjs, uglify, digest, gzip)
pipelineStages := Seq(digest, gzip)

TwirlKeys.templateImports ++= Seq("views.Helpers")

// Disable documentation creation
sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false