val runJVM = taskKey[Unit]("Runs the JVM code")

val compileResourceJS = taskKey[Unit]("Compiles to JavaScript and moves the results to the resource directory")
compileResourceJS := {
  val resultFile = ((fastOptJS in Compile) in fooJS).value.data
  val resourceDir = ((resourceDirectory in Compile) in fooJVM).value
  IO.copyFile(resultFile, resourceDir / "main-fastOpt.js")
}

scalaVersion in ThisBuild := "2.11.6"

lazy val root = project.in(file(".")).
  aggregate(fooJS, fooJVM).
  settings(
    publish := {},
    publishLocal := {},
    runJVM := ((run in Compile) in fooJVM).toTask("").value,
    (run in Compile) := {
      compileResourceJS.value
      runJVM.value
    }
  )

lazy val foo = crossProject.in(file("."))
  .settings(
    organization := "foo",
    name := "foo",
    autoCompilerPlugins := true,
    libraryDependencies ++= Seq(
      "hokko" %%% "hokkonat" % "0.1-SNAPSHOT",
      "com.lihaoyi" %%% "scalatags" % "0.5.5",
      "com.lihaoyi" %%% "sourcecode" % "0.1.0"
    )
  )
  .jvmSettings(
  )
  .jsSettings(
    // scalaJSUseRhino in Global := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.2"
    )
  )


// Needed, so sbt finds the projects
lazy val fooJVM = foo.jvm
lazy val fooJS = foo.js
