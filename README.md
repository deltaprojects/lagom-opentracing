# OpenTracing for the Lagom Framework
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.deltaprojects.lagom-opentracing/lagom-opentracing/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.deltaprojects.lagom-opentracing/lagom-opentracing)

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
    libraryDependencies += "com.deltaprojects" %% "lagom-opentracing" % "0.1.0"
```

When loading your services you should register a global tracer
```scala
val tracer: Tracer = ... // your favourite Tracer implementation

GlobalTracer.register(tracer)
```

### Server Usage
```scala

  override def yourServiceHandler: ServerServiceCall[Request, Response] =
    trace("OPERATION_NAME") { implicit scope =>
      ServerServiceCall { request =>
        scope.span.setBaggageItem("user", request.user)
        ...
      }
    }

```

Note that there is no need to close the scope manually, it will be closed automatically when the handler returns.

### Client Usage

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

## TODO
- [ ] Implement Tests
- [ ] Support adding arbitrary OpenTracing header Tags
- [ ] Support the Java API

Pull requests welcome! :)