package io.hydrosphere.serving.kafka.it

import java.util.concurrent.TimeUnit

import envoy.api.v2.{AggregatedDiscoveryServiceGrpc, DiscoveryRequest, DiscoveryResponse, Node}
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.scala.Logging
import org.scalatest.{FlatSpec, Matchers}

class ClientSpec extends FlatSpec
  with Logging
  with Matchers {

  "kafka container" should "topic should be created" in {

    implicit val modelsChannel = ManagedChannelBuilder
      .forAddress("localhost", 8081)
      .usePlaintext(true)
      .build


    val service = AggregatedDiscoveryServiceGrpc.stub(modelsChannel)

    //    val service = new ModelService(modelsChannel)

    import scala.concurrent.duration._

    val respObserver = new StreamObserver[DiscoveryResponse] {
      override def onError(t: Throwable): Unit = {
        logger.error("!!!!!!!!!!!!!!!!!!!ERROR", t)
      }

      override def onCompleted(): Unit = {
        logger.info("!!!!!!!!!!!!!!!!!!!END")
      }

      override def onNext(value: DiscoveryResponse): Unit = {
        logger.info(s"!!!!!!!!!!!!!!!!!!!VAAAAAAAALLLLLUE: $value")
      }
    }

    val request = service.streamAggregatedResources(respObserver)
    //
    request.onNext(
      DiscoveryRequest(
        versionInfo = "1518461963723",
        node = Some(
          Node(
//            id="applications"
          )
        ),
        //resourceNames = Seq("one", "two"),
        typeUrl = "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application"))
    //request.onCompleted()

    logger.info("completed")
    //request.onNext(DiscoveryRequest(typeUrl = "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application"))
    TimeUnit.SECONDS.sleep(3)

    val stop = 123;



    //    val ddd = service.predict(PredictRequest())
    //    Await.result(ddd, 10 seconds)
    val stop1 = 123;

  }

}
