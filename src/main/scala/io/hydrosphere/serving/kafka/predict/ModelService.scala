package io.hydrosphere.serving.kafka.predict

import io.grpc.Channel
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

import scala.concurrent.Future

class ModelService(channel: Channel) extends PredictionServiceGrpc.PredictionService {

  private[this] val stub = PredictionServiceGrpc.stub(channel)
  override def predict(request: PredictRequest): Future[PredictResponse] = stub.predict(request)

}
