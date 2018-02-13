package io.hydrosphere.serving.kafka.predict

import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph

case class Application(id:Long,
                       name: String,
                       inTopic: Option[String],
                       outTopic: Option[String],
                       errorTopic: Option[String],
                       consumerId: Option[String],
                       executionGraph: Option[ExecutionGraph])



