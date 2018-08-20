package com.deltaprojects.lagom.opentracing

import io.opentracing.{Scope, SpanContext}
import io.opentracing.propagation.{Format, TextMapExtractAdapter, TextMapInjectAdapter}
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer

trait TraceCQRS[+E] {
  val info: java.util.concurrent.ConcurrentHashMap[String, String] = new java.util.concurrent.ConcurrentHashMap[String, String]()
  def withTracing: this.type = {
    val tracer = GlobalTracer.get()
    if (tracer.activeSpan() != null) {
      Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_PRODUCER)

      tracer.inject(tracer.activeSpan().context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(info))
      this
    } else {
      this
    }
  }

  def extractScope(opName: String): Scope = {
    if (GlobalTracer.isRegistered && GlobalTracer.get() != null) {
      val tracer = GlobalTracer.get()
      val activeSpan = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(info))
      activeSpan match {
        case p: SpanContext => tracer.buildSpan(opName).asChildOf(p).withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_CONSUMER).startActive(false)
        case _ => tracer.buildSpan(opName).withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_CONSUMER).startActive(false)
      }
    } else {
      throw new NotImplementedError("No tracer found when extracting tracing information")
    }
  }
}

trait TracedEvent[E] extends TraceCQRS[E] {}

trait TracedCommand[E] extends TraceCQRS[E] {}