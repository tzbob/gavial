---
layout: docs
title: Setup
---

## Getting Started

Kooi relies on scala.js and
scalajs-bundler, add the following plugins to your ```plugins.sbt```:

```scala
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.12.0")
```

The current version of kooi uses macro annotations for easy interoperability,
make sure you add the following dependency to ```build.sbt```:

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
libraryDependencies ++= "be.tzbob" %%% "kooi" % "0.3.9-SNAPSHOT"
```


