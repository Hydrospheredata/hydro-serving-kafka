package io.hydrosphere.serving.kafka.predict

import io.hydrosphere.serving.kafka.kafka_messages.{KafkaError, KafkaServingMessage}
import io.hydrosphere.serving.kafka.utils.KafkaMessageUtils
import io.hydrosphere.serving.manager.grpc.applications.ExecutionStage
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import KafkaMessageUtils._
import cats.instances.future._




trait PredictService {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  def fetchPredict(in: PredictRequest): Future[PredictResponse]

  def calculateFirst(initial: KafkaServingMessage, app: Application):(FutureM, List[ExecutionStage]) = {
    val stages = app.executionGraph.get.stages.toList

    val future = if (stages.isEmpty) Future.successful(KafkaMessageUtils.withException("Empty initial request"))
    else Future.successful(initial
      .forApplicaton(app)
      .forNewStage(stages.head))

    val first = fetchPredictAndEnrich(future, stages.head)

    (first, stages.tail)
  }


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
    val (first, graph) = calculateFirst(initial, app)
    graph.foldLeft(first::Nil)(fetchPredictAndEnrichSeq)
  }

  def fetchPredictAndEnrich(futureMessage :FutureM, stage:ExecutionStage):FutureM = futureMessage.flatMap {
    prevMessage => prevMessage
      .forNewStage(stage)
      .applyIfRequest(fetchPredict) recover {case e:Exception =>
        prevMessage.withError(
          KafkaError(errorMessage = e.getMessage, lastKnownRequest = prevMessage.requestOrError.request)
        )
    }
  }

  def fetchPredictAndEnrichSeq(messages:FutureMList, stage:ExecutionStage):FutureMList = messages match {
    case head::_ => fetchPredictAndEnrich(head, stage)::messages
    case _ => Future.successful(KafkaMessageUtils.withException("Empty initial request").forNewStage(stage)) :: Nil
  }

}
