package io.hydrosphere.serving.kafka.predict

import java.util.concurrent.TimeUnit

import envoy.api.v2.{AggregatedDiscoveryServiceGrpc, DiscoveryRequest, DiscoveryResponse, Node}
import io.grpc.{Channel, ConnectivityState}
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.grpc.applications.{Application => ProtoApplication}
import org.apache.logging.log4j.scala.Logging
import monix.execution.Scheduler.{global => scheduler}
import scala.concurrent.duration._

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

class XDSApplicationUpdateService(implicit chanel: Channel)
  extends UpdateService[Seq[Application]] with Logging{

  import XDSApplicationUpdateService._

  val readyStates = Set(ConnectivityState.READY)
  logger.info(s"Trying to connect to grpc service.")

  /*TODO
  while (!readyStates.contains(chanel.getState(true))){
    logger.info(s"Connecting to grpc service. Current state is ${chanel.getState(true)}")
    TimeUnit.SECONDS.sleep(3)
  }

  logger.info(s"Successfully connected to grpc service: ${chanel.getState(true)}")*/

  val xDSStream: AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub = AggregatedDiscoveryServiceGrpc.stub(chanel)

  val typeUrl = "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application"

  val request = new StreamObserver[DiscoveryResponse] {
    override def onError(t: Throwable): Unit = {
      logger.error("Application stream exception", t)
      scheduler.scheduleOnce(1 seconds) {
        getUpdates()
      }

    }

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

  @annotation.tailrec
  private def retry[T](n: Int, sleep:Int = 1)(fn: => T): T = {
    val r = try { Some(fn) } catch { case e: Exception if n > 1 => None }
    r match {
      case Some(x) => x
      case None => {
        TimeUnit.SECONDS.sleep(sleep)
        logger.warn(s"Retrying to connect to stream ${n - 1} time")
        retry(n - 1)(fn)
      }
    }
  }

  val response: StreamObserver[DiscoveryRequest] = retry(10){
    xDSStream.streamAggregatedResources(request)
  }

  override def getUpdates(): Unit = response.onNext{

    val prevVersion = getVersion()

    logger.info(s"requesting state update. Current version: $prevVersion")

    DiscoveryRequest(
      versionInfo = prevVersion,
      node = Some(
        Node(
          //            id="applications"
        )
      ),
      //resourceNames = Seq("one", "two"),
      typeUrl = "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application")

  }

}
