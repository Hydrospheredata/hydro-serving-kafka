package io.hydrosphere.serving.kafka.predict

import java.util.concurrent.TimeUnit

import io.grpc.{ManagedChannel}
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

import scala.concurrent.Future

class PredictServiceImpl(implicit channel: ManagedChannel) extends PredictService {

  private[this] val stub = PredictionServiceGrpc.stub(channel)
  override def fetchPredict(in: PredictRequest): Future[PredictResponse] = stub.predict(in)

  def close(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

}
