# Gavial

Developing web applications requires dealing with their distributed nature and the natural asynchronicity of user input and network communication.
For facilitating this, different researchers have explored the combination of a multi-tier programming language and functional reactive programming.

__Gavial__ is an embedded domain specific language in Scala that gives developers a language with which web applications can be created by declaratively composing events and behaviors.
Gavial applications are functional reactive programs and web apps in Gavial are a composition of client/server events or behaviors.

## Getting started

To get started with writing Gavial applications you need the [scala build tool](https://www.scala-sbt.org) and [npm](https://www.npmjs.com).
Given an appropriate build file, SBT will pull down all requires libraries and the appropriate Scala version for Gavial applications.
NPM is required since there are some client-side libraries that are used by Scala.js dependencies of Gavial.

### Starting a new project

A new project can be created through the following sbt command:
```
sbt new tzbob/gavial.g8
```
After this command finishes loading (which may take a while if you use SBT for the first time) you should see a prompt to choose a name for your new project and afterwards the name of the organization:

```
name [new-app]: hello <enter>
organization [com.example]: com.example <enter>
```
With 'hello' and 'com.example' as values, the tool creates your project in `./hello`. The directory tree of the template is the following:

```
hello
├── build.sbt
├── project
│   ├── build.properties
│   └── plugins.sbt
└── shared
    └── src
        └── main
            └── scala
                └── HelloApp.scala
```

`build.sbt` contains the build file and configures the project in such a way that is compatible with Gavial's multi-tier Scala implementation.
By default, the template creates a file in the root of the sources, in this case `HelloApp.scala`.
This file contains basic information to get your first Gavial application up and running:

```scala
import mtfrp.core._
import UI.html.all._

object HelloApp extends GavialApp {
  val port = 8080
  val host = "localhost"

  val headExtensions = List(script(src := s"hello-fastopt.js"))

  val ui = ClientDBehavior.constant {
    div(h1("Hello hello!"),
        p("Enjoy Gavial!"))
  }
}
```

The first two lines are imports:

- ```mtfrp.core``` is the package that contains all core functionality of Gavial, the multi-tier FRP framework
- ```UI.html.all``` contains all HTML tags, these implement an existing tags library that is heavily used in the Scala.js community, for more information regarding them we refer to their official documentation: [scalatags](https://www.lihaoyi.com/scalatags/)

The third line defines the application, it extends from `GavialApp`. A `GavialApp` requires implementations for `port`, `host`, `headExtensions` and `ui`:

- `port` and `host` define the port and host on which the web server is bound, defaults in the template are the typical "localhost" and 8080
- `headExtensions` are the extra additions to the `head` tag of the `Gavial` web page, in the template we already fill in the script tag that actually contains the client-side compilation results of `GavialApp`s --- `fastopt` for development mode (faster compile times, bigger JavaScript output) and `fullopt` for production mode (slower compile time, much smaller output)
- `ui` defines a complete `GavialApp`, it is a `ClientDBehavior[HTML]` --- the example shows a simple constant behavior with some 'hello world' text

### Running a Gavial Application

The application created through the Gavial.g8 template is setup to compile both sides of the program at once.
You can start SBT through the `sbt` command and simply issue the `run` command to get the program compiled (after fetching all dependencies) and running.

>  TIP: `run` blocks the SBT shell, you can use `reStart` instead, it runs the application in its separate JVM. You can even use `~reStart` which will restart the application whenever sources change.

Before the application is running it should tell you in which mode it is running; websockets or xhr. After starting up it says at which location it is available, by default at: http://localhost:8080/

### Creating a counter

Let us expand the hello world template to a shared counter:

```scala
val counterSource = ClientEvent.source[Int]

// count on the server
val sessionCounts  = ClientEvent.toSession(counterSource)
val sessionCounter = sessionCounts.fold(0)(_ + _).toDBehavior

// count for everyone!
val globalCounter = SessionDBehavior.toApp(sessionCounter).map(_.values.sum)

// show everyone the global count
val clientCounter = SessionDBehavior.toClient(AppDBehavior.toSession(globalCounter))
val ui = clientCounter.map { c =>
  div(h1("Count!"),
      button("+", UI.listen(onclick, counterSource)(_ => 1)),
      button("-", UI.listen(onclick, counterSource)(_ => -1)),
      p(c)
  )
}
```

First, we create a source to receive counter information.
Next, we replicate the counter events to the session tier after which we fold them into a behavior.
We replicate to the application tier as well since we want a global counter, we do this by merging all individual counters into one by picking just the values of the application replication and making a sum.
We take this global counter result back to the client tier (through the session tier) and map an interface over it.
In this interface we bind our previously created source to the `onclick` attribute on two separate buttons and complete our application.
