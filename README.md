# OpenTracing for the Lagom Framework
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.deltaprojects/lagom-opentracing_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.deltaprojects/lagom-opentracing)

> Note that this software comes without warranty. It is used in production at Delta Projects, but it may not be suited for your use case.
> Lightbend provides commercial OpenTracing support in the form of Lightbend Telemetry https://developer.lightbend.com/docs/cinnamon/2.5.x/home.html

## Lagom

Lagom is a microservices framework by Lightbend for Scala and Java based on Akka and Play. For more information, visit https://www.lagomframework.com/.

## OpenTracing

OpenTracing is a vendor neutral specification for tracing distributed systems. For more information, visit http://opentracing.io/.

## Lagom + OpenTracing

This package provides simple helper methods for dealing with the OpenTracing headers in Lagom service calls.

### SBT

```sbt

libraryDependencies += "com.deltaprojects" %% "lagom-opentracing" % "0.2.2"

```

When loading your services you should register a global tracer
```scala

val tracer: Tracer = ... // your favourite Tracer implementation

GlobalTracer.register(tracer)

```

### HTTP requests
#### Server Usage
```scala

class HelloServiceImpl extends HelloService {
  override def sayHello = ServiceCall { name =>
    Future.successful(s"Hello $name!")
  }
}
  override def yourServiceHandler: ServerServiceCall[Request, Response] =
    trace("OPERATION_NAME") { scope =>
      ServerServiceCall { request =>
        scope.span.setBaggageItem("user", request.user)
        ...
      }
    }

```

Note that there is no need to close the scope manually, it will be closed automatically when the handler returns.

#### Client Usage

```scala

val scope = tracer.buildSpan("Handling your service").startActive(true)

yourServiceClient
.handleRequestHeaders(addTracingHeaders)
.yourServiceHandler
.invoke()
.map(response => {
    scope.span.log("Received response")
    ...
    scope.close()
})

```

### CQRS

You can also trace commands and domain events. Commands and events have the same API. The examples below are from the official [Lagom documentation](https://www.lagomframework.com/documentation/1.4.x/scala/Home.html), sprinkled with some tracing.

#### Command and Event Usage

```scala

final case class AddPost(content: PostContent) extends BlogCommand with TracedCommand[BlogCommand] with ReplyType[AddPostDone]

sealed trait BlogEvent extends AggregateEvent[BlogEvent] with TracedEvent[BlogEvent] {
  override def aggregateTag: AggregateEventShards[BlogEvent] = BlogEvent.Tag
}

final case class PostAdded(postId: String, content: PostContent) extends BlogEvent


override def addPost(id: String) = ServiceCall { request =>
  val ref = persistentEntities.refFor[Post](id)
  ref.ask(request.withTracing).map(ack => "OK") // with tracing!
}
...

override def behavior: Behavior =
  Actions()
    .onCommand[AddPost, AddPostDone] {
      case (com@AddPost(content), ctx, state) if state.isEmpty =>
        val scope = com.extractScope("AddPost")
        ctx.thenPersist(PostAdded(entityId, content).withTracing) { evt => // with more tracing!
          scope.span.finish()
          scope.close()
          ctx.reply(AddPostDone(entityId))
        }
    }
    .onEvent {
      case (ev@PostAdded(postId, content), state) =>
        val scope = ev.extractScope("PostAdded")
        scope.span.setBaggageItem("content", content)
        scope.span.finish()
        scope.close()
        BlogState(Some(content), published = false)
    }
    .onReadOnlyCommand[GetPost.type, PostContent] {
      case (com@GetPost, ctx, state) if !state.isEmpty =>
        val scope = com.extractScope("GetPost")
        scope.span.finish()
        scope.close()
        ctx.reply(state.content.get)
    }

```

## TODO
- [x] Support HTTP tracing
- [x] Support CQRS tracing
- [ ] Implement Tests
- [ ] Support adding arbitrary OpenTracing header Tags
- [ ] Support the Java API

Pull requests welcome! :)