package io.hydrosphere.serving.kafka.utils

import io.grpc.{Server, ServerBuilder, ServerInterceptor, ServerServiceDefinition}

final case class BuilderWrapper[T <: ServerBuilder[T]](builder: ServerBuilder[T]) {
  def addService(service: ServerServiceDefinition): BuilderWrapper[T] = {
    BuilderWrapper(builder.addService(service))
  }

  def intercept(service: ServerInterceptor): BuilderWrapper[T] = {
    BuilderWrapper(builder.intercept(service))
  }

  def build: Server = {
    builder.build()
  }
}