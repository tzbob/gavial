scalaVersion in ThisBuild := "2.11.8"
scalafmtConfig in ThisBuild := Some(file(".scalafmt.conf"))

lazy val root = project
  .in(file("."))
  .aggregate(fooJS, fooJVM)
  .settings(publish := {}, publishLocal := {})

lazy val foo = crossProject
  .in(file("."))
  .settings(
    organization := "foo",
    name := "foo",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Yinline-warnings",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused-import",
      "-Xfuture",
      "-Xlint",
      "-Xfatal-warnings",
      "-language:higherKinds",
      "-language:existentials"
    ),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats"      % "0.7.2",
      "biz.enef" %%% "slogging"       % "0.5.0",
      "be.tzbob" %%% "hokko"          % "0.3.1-SNAPSHOT",
      "com.lihaoyi" %%% "scalatags"   % "0.6.0",
      "org.scalatest" %%% "scalatest" % "3.0.0-M10" % "test"
    ) ++ Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map(_ % Version.circe)
  )
  .jvmSettings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "de.heikoseeberger" %% "akka-sse"          % "1.8.1",
      "de.heikoseeberger" %% "akka-http-circe"   % "1.9.0",
      "com.typesafe.akka" %% "akka-http-testkit" % Version.akka
    )
  )
  .jsSettings(
    scalaJSUseRhino in Global := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    jsDependencies += RuntimeDOM,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "be.tzbob" %%% "scalatags-vdom"  % "0.2-SNAPSHOT"
    )
  )

// Needed, so sbt finds the projects
lazy val fooJS = foo.js.enablePlugins(ScalaJSWeb)

lazy val fooJVM = foo.jvm
  .settings(
    scalaJSProjects := Seq(fooJS),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    managedClasspath in Runtime += (packageBin in Assets).value
  )
  .enablePlugins(SbtWeb)
