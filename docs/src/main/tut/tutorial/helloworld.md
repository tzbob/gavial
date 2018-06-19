---
layout: docs
title: Hello World
---

# Introduction by Example

In kooi you write client / server web applications in Scala by composing 
behaviors and events from functional reactive programming. 

In this introduction we speed through the features of kooi by building a 
naive collaborative todo list.

## Static HTML

We start with the simplest version of the application, a static HTML page.
First, we import the core library and the HTML DSL:

```tut:silent
import mtfrp.core._
import UI.html.all._
```

We define ```ui``` as a constant value and use the HTML DSL to define the page.

```tut:silent
object TodoFRP extends MyMain {
  val ui = ClientDBehavior.constant {
    div(id := "app", 
      h1("Todo!"),
      ul(li("Finish this app!"))
    )
  }
}
```

HTML tags are created by using predefined functions that are named 
accordingly, e.g., ```button("hello")``` can be interpreted as 
```<button>hello</button>```. Attribute pairs are defined by combining 
predefined attribute keys with values, e.g., ```src := "test.jpg"```.
Our DSL implements the same interface as 
[scalatags](http://www.lihaoyi.com/scalatags/), for extended examples you can consult their tutorial.

### MyMain

Kooi applications fill in the ```MyMain``` trait and define an application as
 a ```ClientDBehavior``` of ```HTML``` elements:

```tut:silent
trait MyMain {
  def ui: ClientDBehavior[UI.HTML]
}
```

All applications are written as compositions of events and behaviors. A detailed
overview of events, behaviors and their operations is available [here](). Events
are sporadic values, they are streams that are populated when something happens.
Behaviors describe a value and its changes, unlike events, it always holds a
'current' value. Behaviors come in three shapes depending on *how* it changes,
continuous (no information on when it changes), discrete (changes sporadically
at known times, i.e., an event which holds a value) and incremental (changes
sporadically *and* incrementally).

Note that `ui` is a discrete behavior, that is, a sporadically changing value.
It requires a *discrete* behavior of HTML elements since kooi needs to know
*when* something changed to update the DOM.

## Dynamic HTML

A static HTML page is hardly interesting. Let us abstract over entries by
creating a case class. `Entry` is defined by a boolean `done` and a string
`text`. It creates an HTML value `template` which is an appropriate list
item that contains either `"done"` or `"todo"` as a css class:

```tut:silent
case class Entry(done: Boolean, text: String) {
  val doneCls = if (done) "done" else "todo"
  val template = li(`class` := doneCls, text)
}
```

We can rewrite the static example using a list of entries. We define a constant
behavior of entries `todoState` and map it to the expected user interface. This
pattern of modeling and separating *changing* state will appear often in kooi
applications since it makes the separation of the user interface natural and
easy.

```tut:silent
object TodoFRP extends MyMain {
  val entryList = List(Entry(false, "Finish this app!"))
  val todoState = ClientDBehavior.constant(entryList)

  val ui = todoState.map { todos =>
    div(id := "app", 
      h1("Todo!"),
      ul(todos.map(_.template))
    )
  }
}
```

However, our application is still static. No entries can be added and apart from
using `todoState` to define `ui` nothing really changed.

Let us make the list extendable by adding a textbox. To make this possible we
have to rewrite the application to:
* deal with user events
* make `todoState` dynamic

First of, we create an event source `submitPress` and a behavior sink
`entryText`. Event sources are used to retrieve DOM events as FRP events and
behavior sinks can be used to read DOM properties. However, since behaviors are
always defined (even if they're not reading any properties!) you need to supply
it with a default value.

Next, we model a user submission to the system. In this case, it would be the
*value* of the inputbox at the *time* of a submit


```tut:silent
object TodoFRP extends MyMain {
  val submitPress = ClientEvent.source[Unit]
  val entryText   = ClientBehavior.sink("")

  val entrySubmission: ClientEvent[Entry] = 
    entryText.snapshotWith(submitPress) { (text, _) =>
      Entry(false, text)
    }

  val todoState: ClientIBehavior[List[Entry], Entry] = 
    entrySubmission.fold(List.empty[Entry])(_ :+ _)

  val ui = todoState.toDBehavior.map { todos =>
    val rawInput = input(`type` := "text", value := "")
    val readInput = UI.read(rawInput)(entryText, el => {
      el.value.asInstanceOf[String]
    })
  
    div(id := "app", 
      h1("Todo!"),
      ul(todos.map(_.template)),
      form(action := "", UI.listen(onsubmit, submitPress)(_ => ()),
        readInput
      )
    )
  }
}
```


