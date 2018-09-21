import microsites._

resolvers in ThisBuild += "Sonatype OSS Snapshots" at
  "https://oss.sonatype.org/content/repositories/snapshots"

organization in ThisBuild := "be.tzbob"
scalaVersion in ThisBuild := "2.12.4"
crossScalaVersions in ThisBuild := Seq("2.11.12", "2.12.4")
version in ThisBuild := "0.3.9-SNAPSHOT"

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
  "-language:higherKinds",
  "-Ypartial-unification"
)

lazy val multitier = crossProject
  .in(file("multitier"))
  .settings(
    name := "kooi",
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"   % "1.0.1",
      "biz.enef"      %%% "slogging"    % "0.5.3",
      "be.tzbob"      %%% "hokko"       % "0.4.9-SNAPSHOT",
      "com.lihaoyi"   %%% "scalatags"   % "0.6.3",
      "org.typelevel" %%% "cats-effect" % "0.9",
      "org.scalatest" %%% "scalatest"   % "3.0.1" % "test"
    ) ++ Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map(_ % Version.circe)
  )
  .jvmSettings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"         % "10.0.9",
      "com.typesafe.akka" %% "akka-http-testkit" % "10.0.9" % Test
    )
  )
  .jsSettings(
    requiresDOM in Test := true,
    webpackBundlingMode in Test := BundlingMode.LibraryAndApplication(),
    npmDependencies in Test += "event-source-polyfill" -> "0.0.9",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom"     % "0.9.1",
      "be.tzbob"     %%% "scalatags-hokko" % "0.3.5-SNAPSHOT"
    )
  )

lazy val macros = crossProject
  .in(file("macros"))
  .settings(
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
lazy val macrosJS  = macros.js
lazy val macrosJVM = macros.jvm

// Needed, so sbt finds the projects
lazy val multitierJS = multitier.js
  .enablePlugins(ScalaJSBundlerPlugin)
  .dependsOn(macrosJS)

lazy val multitierJVM = multitier.jvm.dependsOn(macrosJVM)

lazy val docs = project
  .in(file("docs"))
  .settings(
    micrositeName := "Kooi",
    micrositeDescription := "Multi-tier functional reactive programming for " +
      "Scala",
    micrositeHighlightTheme := "atom-one-light",
    micrositePalette := Map(
      "brand-primary"   -> "#7F004B",
      "brand-secondary" -> "#7F265B",
      "brand-tertiary"  -> "#5E264B",
      "gray-dark"       -> "#49494B",
      "gray"            -> "#7B7B7E",
      "gray-light"      -> "#E5E5E6",
      "gray-lighter"    -> "#F4F3F4",
      "white-color"     -> "#FFFFFF"
    ),
    micrositeExtraMdFiles := Map(
      file("README.md") -> ExtraMdFileConfig("index.md", "home")
    ),
    micrositeDocumentationUrl := "/tutorial/"
  )
  .enablePlugins(MicrositesPlugin)
  .dependsOn(multitierJS, multitierJVM)
