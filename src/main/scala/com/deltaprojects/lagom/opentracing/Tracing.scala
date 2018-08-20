package com.deltaprojects.lagom.opentracing

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import io.opentracing.contrib.concurrent.TracedExecutionContext
import io.opentracing.propagation.{Format, TextMap, TextMapExtractAdapter}
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer
import io.opentracing.{Scope, SpanContext, Tracer}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// adapted from https://github.com/yurishkuro/opentracing-tutorial/tree/master/java/src/main/java/lesson03
private[lagom] class RequestBuilderCarrier(val headers: RequestHeader) extends TextMap {
  private var h = headers
  override def iterator = throw new UnsupportedOperationException("carrier is write-only")

  override def put(key: String, value: String): Unit = {
    h = h.addHeader(key, value)
  }

  def get: RequestHeader = h
}

object Tracing {
  private val logger = LoggerFactory.getLogger(Tracing.getClass)

  /**
    * Add tracing information to request headers
    * @param requestHeader - request headers
    * @return request headers with tracing information
    */
  def addTracingHeaders(requestHeader: RequestHeader): RequestHeader = {
    val tracer: Tracer = GlobalTracer.get()
    try {
      if (tracer.activeSpan() != null) {
        Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT)
        Tags.HTTP_METHOD.set(tracer.activeSpan(), requestHeader.method.name)
        Tags.HTTP_URL.set(tracer.activeSpan(), requestHeader.uri.toString)

        val carrier = new RequestBuilderCarrier(requestHeader)
        tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS, carrier)
        carrier.get
      } else {
        logger.warn("No active span found when adding tracing headers. Did you forget to activate a span?")
        requestHeader
      }
    } catch {
      case e:Throwable =>
        logger.error(e.getMessage)
        requestHeader
    }
  }

  def trace[Request, Response](serviceCall: Scope => ServerServiceCall[Request, Response])
    (implicit executionContext: ExecutionContext): ServerServiceCall[Request, Response] = trace("Unnamed trace")(serviceCall)

  /**
    * Service call composition to enhance service implementation with active scope.
    * @param opName - Operation name
    * @param serviceCall - ServerServiceCall
    * @param executionContext - implicit execution context
    * @tparam Request - Lagom Request
    * @tparam Response - Lagom Response
    * @return Enhanced ServerServiceCall
    */
  def trace[Request, Response](opName: String)(serviceCall: Scope => ServerServiceCall[Request, Response])
    (implicit executionContext: ExecutionContext): ServerServiceCall[Request, Response] =
    ServerServiceCall.compose { requestHeader =>
      import scala.collection.JavaConverters._
      if (GlobalTracer.isRegistered && GlobalTracer.get() != null) {
        val tracer: Tracer = GlobalTracer.get()
        try {
          val parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(requestHeader.headers.toMap.asJava))
          val scope = parentSpan match {
            case p: SpanContext => tracer.buildSpan(opName).asChildOf(p).withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_SERVER).startActive(false)
            case _ => tracer.buildSpan(opName).withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_SERVER).startActive(false)
          }
          val tracedExecutionContext = new TracedExecutionContext(executionContext)
          new ServerServiceCall[Request, Response] {
            override def invoke(request: Request): Future[Response] = {
              serviceCall(scope).invoke(request).andThen {
                case Success(resp) =>
                  scope.span().setTag(Tags.HTTP_STATUS.getKey, 200)
                  scope.span().finish()
                  scope.close()
                  resp
                case Failure(t) =>
                  scope.span().setTag(Tags.ERROR.getKey, true)
                  scope.span().finish()
                  scope.close()
                  t
              }(tracedExecutionContext)
            }
          }
        } catch {
          case _: IllegalArgumentException => serviceCall(tracer.buildSpan(opName).startActive(false))
        }
      } else {
        logger.error("No Tracer registered when trying to trace!")
        throw new NotImplementedError("No Tracer registered")
      }
    }
}
