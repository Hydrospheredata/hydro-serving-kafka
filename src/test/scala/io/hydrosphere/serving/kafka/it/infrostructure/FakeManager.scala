package io.hydrosphere.serving.kafka.it.infrostructure

import io.grpc.{Server, ServerBuilder}
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

object FakeModel extends Logging {

  def run(): Unit = {
    val server = new FakeModel(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class FakeModel (executionContext: ExecutionContext) extends Logging { self =>

  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(FakeModel.port).addService(PredictionServiceGrpc.bindService(new PredictionService, executionContext)).build.start
    logger.info("Server started, listening on " + FakeModel.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class PredictionService extends PredictionServiceGrpc.PredictionService {
    override def predict(request: PredictRequest): Future[PredictResponse] = Future.successful(PredictResponse(request.inputs))
  }
}
