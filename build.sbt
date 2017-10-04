resolvers in ThisBuild += "Sonatype OSS Snapshots" at
  "https://oss.sonatype.org/content/repositories/snapshots"

organization in ThisBuild := "be.tzbob"
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.1")
version in ThisBuild := "0.3.1-SNAPSHOT"

scalacOptions in ThisBuild ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-deprecation",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-language:higherKinds"
)

lazy val multitier = crossProject
  .in(file("."))
  .settings(
    name := "kooi",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats"      % "0.9.0",
      "biz.enef"      %%% "slogging"  % "0.5.3",
      "be.tzbob"      %%% "hokko"     % "0.4.3-SNAPSHOT",
      "com.lihaoyi"   %%% "scalatags" % "0.6.3",
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
    ) ++ Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map(_ % Version.circe)
  )
  .jvmSettings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "de.heikoseeberger" %% "akka-http-circe"   % "1.17.0",
      "com.typesafe.akka" %% "akka-http"         % "10.0.9",
      "com.typesafe.akka" %% "akka-http-testkit" % "10.0.9" % Test
    )
  )
  .jsSettings(
    enableReloadWorkflow := true,
    useYarn := true,
    npmDependencies in Test += "event-source-polyfill" -> "0.0.9",
    jsDependencies += RuntimeDOM,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom"     % "0.9.1",
      "be.tzbob"     %%% "scalatags-hokko" % "0.3.1-SNAPSHOT"
    )
  )

// Needed, so sbt finds the projects
lazy val multitierJS = multitier.js
  .enablePlugins(ScalaJSBundlerPlugin)
lazy val multitierJVM = multitier.jvm
