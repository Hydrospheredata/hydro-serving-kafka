package io.hydrosphere.serving.kafka.predict

import envoy.api.v2.{AggregatedDiscoveryServiceGrpc, DiscoveryRequest, DiscoveryResponse, Node}
import io.grpc.{Channel, ManagedChannel}
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.grpc.applications.{Application => ProtoApplication}
import org.apache.logging.log4j.scala.Logging

import scala.collection.Seq
import scala.util.Try


object XDSApplicationUpdateService{
  implicit class ApplicationWrapper(app:ProtoApplication){
    def toInternal():Seq[Application] = app.kafkaStreaming.map {
      kafkaSettings =>
        Application(
          id = app.id,
          name = app.name,
          inTopic = Option(kafkaSettings.sourceTopic),
          outTopic = Option(kafkaSettings.destinationTopic),
          errorTopic = Option(kafkaSettings.errorTopic),
          executionGraph = app.executionGraph,
          consumerId = Option(kafkaSettings.consumerId)
        )
    }
  }
}

class XDSApplicationUpdateService(implicit chanel: ManagedChannel)
  extends UpdateService[Seq[Application]] with Logging{

  import XDSApplicationUpdateService._

  val xDSStream: AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub = AggregatedDiscoveryServiceGrpc.stub(chanel)

  val typeUrl = "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application"

  val request = new StreamObserver[DiscoveryResponse] {
    override def onError(t: Throwable): Unit = logger.error("Application stream exception", t)

    override def onCompleted(): Unit = logger.info("Application stream closed")

    override def onNext(value: DiscoveryResponse): Unit = {
      logger.info(s"Discovery stream update: $value")

      val applications:Seq[Application] = value.resources.flatMap{
        any => Try {
          ProtoApplication.parseFrom(any.value.newInput()).toInternal()
        } recover { case e:Throwable =>
          logger.error("Unable to deserialize message with Application proto", e)
          Seq[Application]()
        } get
      }

      doOnNext(applications, value.versionInfo)
    }
  }

  val response: StreamObserver[DiscoveryRequest] = xDSStream.streamAggregatedResources(request)

  override def getUpdates(version: String): Unit = response.onNext(
    DiscoveryRequest(
      versionInfo = version,
      node = Some(
        Node(
          //            id="applications"
        )
      ),
      //resourceNames = Seq("one", "two"),
      typeUrl = "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application"))
}
