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

  def withException(e: Throwable): KafkaServingMessage = KafkaServingMessage(
    meta = None,
    requestOrError = RequestOrError.Error(KafkaError(
      e.getMessage, None
    ))
  )

  def withException(errorMessage: String): KafkaServingMessage = KafkaServingMessage(
    meta = None,
    requestOrError = RequestOrError.Error(KafkaError(
      errorMessage, None
    ))
  )

  implicit class KafkaServingMessageSyntax(underlying: KafkaServingMessage) {

    def forNewStage(stage: ExecutionStage): KafkaServingMessage = {

      val modelSpecFromStage: Option[ModelSpec] = for {
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
              modelSpecFromStage.getOrElse {
                val modelSpecFromRequest = value
                  .modelSpec

                if (modelSpecFromRequest.isEmpty) throw new IllegalArgumentException(
                  "ModelSpec should be provided by stage or by request(if application has only one stage)"
                )

                modelSpecFromRequest.get
              }
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
        case RequestOrError.Error(_) => Monad[F].pure(underlying)
        case RequestOrError.Empty => Monad[F].pure(withException("Empty request message"))
        case RequestOrError.Request(predictRequest) => Monad[F].map(transform(predictRequest)) { resp => underlying.withRequest(predictRequest.withInputs(resp.outputs)) }

      }

    def forApplicaton(app: Application) = underlying.withMeta {
      underlying
        .meta
        .getOrElse(KafkaMessageMeta())
        .withApplicationId(app.name)
    }.withRequestOrError(
      if (underlying.requestOrError.isRequest) {
        val request = underlying
          .requestOrError
          .request
          .map(r => r.withModelSpec(
            r.modelSpec.getOrElse(ModelSpec()).withName(app.name)
          ))
        RequestOrError.Request(request.get)
      } else underlying.requestOrError)

  }

}
