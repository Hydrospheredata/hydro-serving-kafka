package io.hydrosphere.serving.kafka.it.infrostructure

import com.google.protobuf.any.Any
import envoy.api.v2.{AggregatedDiscoveryServiceGrpc, DiscoveryRequest, DiscoveryResponse}
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.kafka.predict.{Application, XDSApplicationUpdateService}
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage, KafkaStreaming, Application => ProtoApplication}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future, Promise}

object FakeModel extends Logging {

  def run(ppredictPort: Int, appPort: Int): FakeModel = {
    val server = new FakeModel(ExecutionContext.global)
    server.start(ppredictPort, appPort)
    server.blockUntilShutdown()
    server
  }

  def runAsync(predictPort: Int, appPort: Int) = {
    val result = Promise[FakeModel]
    new Thread("fake-model-service") {
      override def run(): Unit = {
        val server = new FakeModel(ExecutionContext.global)
        server.start(predictPort, appPort)
        result.success(server)
        server.blockUntilShutdown()
      }
    }.start()
    result.future
  }
}

class FakeModel(executionContext: ExecutionContext) extends Logging {
  self =>

  private[this] var predictServer: Server = _
  private[this] var appServer: Server = _

  private def start(predictPort: Int, appPort: Int): Unit = {
    predictServer = ServerBuilder.forPort(predictPort).addService(PredictionServiceGrpc.bindService(new PredictionService, executionContext)).build.start
    appServer = ServerBuilder.forPort(appPort).addService(AggregatedDiscoveryServiceGrpc.bindService(new AppService, executionContext)).build.start

    logger.info("Predict Server started, listening on " + predictPort)
    logger.info("App Server started, listening on " + appPort)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def stop(): Unit = {
    if (predictServer != null) {
      predictServer.shutdown()
    }

    if (appServer != null) {
      appServer.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (predictServer != null) {
      predictServer.awaitTermination()
    }

    if (appServer != null) {
      appServer.awaitTermination()
    }
  }

  private class AppService extends AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryService {

    override def streamAggregatedResources(responseObserver: StreamObserver[DiscoveryResponse]): StreamObserver[DiscoveryRequest] = {

      val version = "11111111"

      val app = ProtoApplication(
        id = 111,
        name = "someApp",
        kafkaStreaming = Seq(KafkaStreaming("testConsumer", "test", "success", "failure")),
        executionGraph = Some(ExecutionGraph(
          stages = ExecutionStage("first", Some(ModelSignature(
            signatureName = "first_signature"
          ))) :: ExecutionStage("second", Some(ModelSignature(
            signatureName = "second_signature"
          ))) :: Nil
        ))
      )


      new StreamObserver[DiscoveryRequest] {
        override def onError(t: Throwable): Unit = ???

        override def onCompleted(): Unit = ???

        override def onNext(value: DiscoveryRequest): Unit =
          if (value.versionInfo != version) responseObserver.onNext(
            DiscoveryResponse(
              versionInfo = version,
              resources = Seq(Any.apply(
                "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application",
                app.toByteString
              )
              )
            ))
      }
    }

  }

  private class PredictionService extends PredictionServiceGrpc.PredictionService {

    val applications = List(
      Application(
        123,
        "test-app",
        Some("test"),
        Some("success"),
        Some("failure"),
        Some("testConsumer"),
        Some(ExecutionGraph(Seq(executionGraph(1), executionGraph(2), executionGraph(3)))))
    )

    def executionGraph(num: Int) = (ExecutionStage(
      stageId = s"stage$num",
      signature = Some(ModelSignature(
        signatureName = s"Signature$num"
      ))
    ))

    override def predict(request: PredictRequest): Future[PredictResponse] = Future.successful(PredictResponse(request.inputs))


  }


}
