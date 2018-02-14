package io.hydrosphere.serving.kafka.predict

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaError, KafkaServingMessage}
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try


trait PredictService {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  def fetchPredict(in: PredictRequest): Future[PredictResponse]

  def report(traceId: String, future: Future[PredictResponse]): Future[KafkaServingMessage] = future
    .map(resp => KafkaServingMessage(traceId).withRequest(PredictRequest(inputs = resp.outputs)))
    .recover { case e: Exception => KafkaServingMessage(traceId).withError(KafkaError(
      errorMessage = e.getMessage))
    }

  def predictByGraph(request: PredictRequest, app: Application): Future[PredictResponse] = {
    app.executionGraph.get.stages.toList match {
      case Nil => Future.failed(new RuntimeException("Should be at least one PredictRequest item"))
      case head :: tail =>

        val modelSpec = ModelSpec(
          name = app.name,
          signatureName = head.stageId
        )

        val requestWithSignature = request.withModelSpec(modelSpec)
        predictRec(Future.successful(requestWithSignature), tail)
    }
  }

  @tailrec
  private[this] def predictRec(prev: Future[PredictRequest], stages: Seq[ExecutionStage]): Future[PredictResponse] = {

    val result = prev.flatMap(fetchPredict)

    def toFutureRequest(futureResponse: Future[PredictResponse], modelSignature: ModelSignature): Future[PredictRequest] =
      for {
        response <- futureResponse
        spec <- prev.map(_.modelSpec)
      } yield PredictRequest(
        modelSpec = spec.map(_.copy(signatureName = modelSignature.signatureName)),
        inputs = response.outputs
      )

    stages match {
      case Nil => result
      case head :: tail => predictRec(toFutureRequest(result, head.signature.get), tail)
    }
  }

}
