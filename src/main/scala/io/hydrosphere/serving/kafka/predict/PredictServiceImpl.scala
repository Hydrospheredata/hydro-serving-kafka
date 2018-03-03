package io.hydrosphere.serving.kafka.predict

import io.grpc.Channel
import io.hydrosphere.serving.grpc.AuthorityReplacerInterceptor
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

import scala.concurrent.Future

class PredictServiceImpl(implicit channel: Channel) extends PredictService {

  private[this] val stub = PredictionServiceGrpc.stub(channel)
  override def fetchPredict(in: PredictRequest)(stage:String): Future[PredictResponse] = {
    stub
      .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, stage)
      .predict(in)
  }

  def close(): Unit = {
   //TODO  channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

}
