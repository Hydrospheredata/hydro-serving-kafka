package io.hydrosphere.serving.kafka.predict

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaError, KafkaMessageMeta, KafkaServingMessage}
import io.hydrosphere.serving.kafka.utils.KafkaMessageUtils
import io.hydrosphere.serving.manager.grpc.applications.ExecutionStage
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}


trait PredictService {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  def fetchPredict(in: PredictRequest): Future[PredictResponse]

  /**
    *
    * returns list of stage results as KafkaServingMessage
    * (request with inputs as model calculation result or error with last worked result (if we have it))
    *
    * @param initial KafkaServingMessage with requestOrError enriched with kafka metadata
    * @param app application data for message processing
    * @return list of future messages (every stage result)
    *
    */
  def predictByGraph(initial: KafkaServingMessage, app: Application): FutureMList = {
    val stages = app.executionGraph.get.stages.toList
    stages match {
      case Nil => Future.successful(KafkaMessageUtils.withException("Empty initial request"))::Nil
      case head :: tail =>
        val enrichedMessage = KafkaServingMessage(
          meta = initial.meta.map(_.withApplicationId(app.name)),
          requestOrError = initial.requestOrError match {
            case RequestOrError.Request(r) => {
              val modelSpec = ModelSpec(
                name = app.name,
                signatureName = head.stageId
              )

              val requestWithSignature = r.withModelSpec(modelSpec)
              RequestOrError.Request(requestWithSignature)
            }
            case other:Any => other
          }
        )
        val firstMessage = Future.successful(enrichedMessage)
        predictRec(fetchPredictAndEnrich(firstMessage, head)::Nil, tail)
    }
  }

  def fetchPredictAndEnrich(futureMessage :FutureM, stage:ExecutionStage):FutureM = futureMessage.flatMap {
    prevMessage => prevMessage.requestOrError match {
      case RequestOrError.Error(_) => Future.successful(withNewStage(prevMessage, stage))
      case RequestOrError.Empty => Future.successful(withNewStage(KafkaMessageUtils.withException("Empty request message"), stage))
      case RequestOrError.Request(request) => {
        val newtRequest = request.withModelSpec(request.modelSpec.get.withSignatureName(stage.signature.get.signatureName))
        val futureResponse = fetchPredict(newtRequest)
        val futureRequest = futureResponse.map(resp => newtRequest.withInputs(resp.outputs))
        futureRequest.map {
          request => withNewStage(prevMessage.withRequest(request), stage)
        } recover{ case e:Exception =>  withNewStage(prevMessage.withError(KafkaError(
          errorMessage = e.getMessage,
          lastKnownRequest = Some(request)
        )), stage)}
      }
    }
  }


  def withNewStage(message:KafkaServingMessage, stage:ExecutionStage):KafkaServingMessage =
    message.withMeta(message.meta.map(_.withStageId(stage.stageId)).getOrElse(KafkaMessageMeta()))


  def fetchPredictAndEnrichSeq(messages:FutureMList, stage:ExecutionStage):FutureMList = messages match {
    case head::_ => fetchPredictAndEnrich(head, stage)::messages
    case _ => Future.successful(withNewStage(KafkaMessageUtils.withException("Empty initial request"), stage)) :: Nil
  }

  @tailrec
  private[this] def predictRec(prev: FutureMList, stages: List[ExecutionStage]): FutureMList = {
    stages match {
      case Nil => prev
      case head :: tail => {
        val next = fetchPredictAndEnrichSeq(prev, head)
        predictRec(next, tail)
      }
    }
  }

}
