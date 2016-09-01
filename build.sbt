scalaVersion in ThisBuild := "2.11.8"

lazy val root = project
  .in(file("."))
  .aggregate(fooJS, fooJVM)
  .settings(publish := {}, publishLocal := {})

lazy val foo = crossProject
  .in(file("."))
  .settings(
    scalafmtConfig in ThisBuild := Some(file(".scalafmt")),
    organization := "foo",
    name := "foo",
    autoCompilerPlugins := true,
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-target:jvm-1.6",
      "-feature",
      "-deprecation",
      "-Xlint",
      "-Yinline-warnings",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture",
      "-language:higherKinds",
      "-language:existentials"
    ),
    libraryDependencies ++= Seq(
      "hokko" %%% "hokkonat" % "0.1-SNAPSHOT",
      "com.lihaoyi" %%% "scalatags" % "0.6.0",
      "com.lihaoyi" %%% "sourcecode" % "0.1.0",
      "org.scalatest" %%% "scalatest" % "3.0.0-M10" % "test"
    ) ++ Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map(_ % Version.circe)
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "de.heikoseeberger" %% "akka-sse" % "1.8.1",
      "de.heikoseeberger" %% "akka-http-circe" % "1.8.0",
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
      "be.tzbob" %%% "scalatags-vdom" % "0.2-SNAPSHOT"
    )
  )

// Needed, so sbt finds the projects
lazy val fooJS = foo.js

lazy val fooJVM = foo.jvm.settings(
  resources in Compile += (packageJSDependencies in fooJS in Compile).value,
  resources in Compile += (fastOptJS in fooJS in Compile).value.data,
  resources in Compile += (packageScalaJSLauncher in fooJS in Compile).value.data
)
