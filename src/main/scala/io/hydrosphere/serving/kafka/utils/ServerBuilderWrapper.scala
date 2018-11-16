package io.hydrosphere.serving.kafka.utils

import io.grpc._

final case class ServerBuilderWrapper[T <: ServerBuilder[T]](builder: ServerBuilder[T]) {
  def addService(service: ServerServiceDefinition): ServerBuilderWrapper[T] = {
    ServerBuilderWrapper(builder.addService(service))
  }

  def intercept(service: ServerInterceptor): ServerBuilderWrapper[T] = {
    ServerBuilderWrapper(builder.intercept(service))
  }

  def build: Server = {
    builder.build()
  }
}

final case class ChannelBuilderWrapper[T <: ManagedChannelBuilder[T]](builder: ManagedChannelBuilder[T]) {
  def intercept(clientInterceptor: ClientInterceptor*) = {
    ChannelBuilderWrapper(builder.intercept(clientInterceptor :_*))
  }

  def build = builder.build()
}