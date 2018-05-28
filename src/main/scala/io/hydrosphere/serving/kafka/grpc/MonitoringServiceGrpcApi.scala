package io.hydrosphere.serving.kafka.grpc

import com.google.protobuf.empty.Empty
import io.hydrosphere.serving.grpc.Headers
import io.hydrosphere.serving.kafka.stream.Producer
import io.hydrosphere.serving.monitoring.monitoring.ExecutionInformation
import io.hydrosphere.serving.monitoring.monitoring.MonitoringServiceGrpc.MonitoringService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

class MonitoringServiceGrpcApi(implicit producer: Producer[Array[Byte], ExecutionInformation]
) extends MonitoringService with Logging {

  implicit val ctx = scala.concurrent.ExecutionContext.global

  override def analyze(request: ExecutionInformation): Future[Empty] = {

    val topic = Option(Headers.XServingKafkaProduceTopic.contextKey.get())
      .getOrElse(throw new IllegalArgumentException("Specify topic"))

    producer.send(topic, request)
      .map(_ => Empty())
  }
}
