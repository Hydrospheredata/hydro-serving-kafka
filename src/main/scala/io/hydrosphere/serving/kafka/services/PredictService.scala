package io.hydrosphere.serving.kafka.services

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}

import scala.annotation.tailrec
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.global

trait PredictService {

  def fetchPredict(in: PredictRequest): Future[PredictResponse]

  def getExecutionGraph(modelSpec:ModelSpec):ExecutionGraph

  def predictByGraph(request:PredictRequest): Future[PredictResponse] = {
    getExecutionGraph(request.modelSpec.get).stages match {
      case Nil => Future.failed(new RuntimeException("Should be at least one PredictRequest item"))
      case head :: tail =>{
        val requestWithSignature = request.withModelSpec(
          request.modelSpec.get.withSignatureName(
            head.signature.get.signatureName)
        )
        predictRec(Future.successful(requestWithSignature), tail)
      }
    }
  }

  @tailrec
  private def predictRec(prev: Future[PredictRequest], stages: Seq[ExecutionStage]): Future[PredictResponse] = {

    implicit val exec = global

    val result = prev.flatMap(fetchPredict(_))

    def toFutureRequest(futureResponse:Future[PredictResponse], modelSignature:ModelSignature):Future[PredictRequest] =
      for{
        response <- futureResponse
        spec <- prev.map(_.modelSpec)
      } yield PredictRequest(
        modelSpec = spec.map(_.copy(signatureName = modelSignature.signatureName)),
        inputs = response.outputs
      )

    stages match {
      case Nil => result
      case head :: Nil => result
      case head :: tail => predictRec(toFutureRequest(result, tail.head.signature.get), tail)
    }
  }

}
