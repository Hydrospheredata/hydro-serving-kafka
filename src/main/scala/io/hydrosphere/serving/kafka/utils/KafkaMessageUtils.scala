package io.hydrosphere.serving.kafka.utils

import cats.{Applicative, Monad}
import cats.implicits._
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaError, KafkaMessageLocation, KafkaMessageMeta, KafkaServingMessage}
import io.hydrosphere.serving.kafka.predict.Application
import io.hydrosphere.serving.manager.grpc.applications.ExecutionStage
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}


object KafkaMessageUtils {

  def withException(e: Throwable): KafkaServingMessage = withException(e.getMessage)

  def withException(errorMessage: String): KafkaServingMessage = KafkaServingMessage(
    meta = None,
    requestOrError = RequestOrError.Error(KafkaError(
      errorMessage, None
    ))
  )

  implicit final class KafkaServingMessageSyntax(val underlying: KafkaServingMessage) extends AnyVal {

    def forNewStage(stage: ExecutionStage): KafkaServingMessage = {

      val modelSpecFromStage = for {
        name <- underlying.meta.map(_.applicationId)
        signatureName <- stage.signature.map(_.signatureName)
      } yield ModelSpec(
        name = name,
        signatureName = signatureName
      )

      underlying
        .withRequestOrError {
          underlying.requestOrError match {
            case RequestOrError.Request(value) => RequestOrError.Request(value.withModelSpec {
              val modelSpec = modelSpecFromStage orElse value.modelSpec
              if (modelSpec.isEmpty) throw new IllegalArgumentException(
                "ModelSpec should be provided by stage or by request(if application has only one stage)"
              )
              modelSpec.get
            })
            case e: RequestOrError.Error => e
            case RequestOrError.Empty => RequestOrError.Error(KafkaError("Empty request message"))
          }
        }.withMeta(underlying.meta.map(_.withStageId(stage.stageId)).getOrElse(KafkaMessageMeta()))

    }

    def withKafkaData(topic: String, partition: Int, offset: Long, consumerId: String) = {
      val meta = underlying.meta.getOrElse(KafkaMessageMeta())
        .withLocation(KafkaMessageLocation(
          sourceTopic = topic,
          partition = partition,
          offset = offset,
          consumerId = consumerId
        ))

      underlying.withMeta(meta)
    }

    def applyIfRequest[F[_] : Monad](transform: PredictRequest => F[PredictResponse]): F[KafkaServingMessage] =
      underlying.requestOrError match {
        case RequestOrError.Error(_) => underlying.pure[F]
        case RequestOrError.Empty => withException("Empty request message").pure[F]
        case RequestOrError.Request(predictRequest) => transform(predictRequest).map {
          resp => underlying.withRequest(predictRequest.withInputs(resp.outputs))
        }
      }

    def forApplicaton(app: Application) = {
      val meta = underlying
        .meta
        .getOrElse(KafkaMessageMeta.defaultInstance)
        .withApplicationId(app.name)

      val requestOrError = underlying.requestOrError match {
        case RequestOrError.Request(request) =>
          val newModelSpec = request.modelSpec.getOrElse(ModelSpec.defaultInstance).withName(app.name)
          RequestOrError.Request(request.withModelSpec(newModelSpec))
        case x => x
      }

      underlying.withMeta(meta).withRequestOrError(requestOrError)
    }
  }
}