package io.hydrosphere.serving.kafka.predict

import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec

case class Application(
                        name:String,
                        inTopic: Option[String],
                        outTopic: Option[String],
                        errorTopic: Option[String],
                        modelSpec: ModelSpec,
                        executionGraph: ExecutionGraph)

