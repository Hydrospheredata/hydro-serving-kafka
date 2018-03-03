package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaMessageMeta, KafkaServingMessage}
import io.hydrosphere.serving.kafka.predict.{Application, PredictService}
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage}
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class PredictServiceSpec
  extends FlatSpec
    with Matchers {

  implicit val ececutionContext: ExecutionContextExecutor = global

  def signature(key: String) = ModelSignature(key)

  def app(stages: Seq[ExecutionStage]) = Application(
    executionGraph = Some(ExecutionGraph(stages)),
    name = "SomeApp",
    id = 1,
    inTopic = None,
    outTopic = None,
    errorTopic = None,
    consumerId = None
  )

  val input: Map[String, TensorProto] = Map[String, TensorProto](("key", TensorProto()))

  "PredictService" should "return successful response for one stage" in {

    val stages: List[ExecutionStage] = ExecutionStage("success", Some(signature("success"))) :: Nil
    val result = resultList(stages)
    result.size shouldBe (stages.size)
    result.map(_.getRequest.inputs.values.map(_.versionNumber)).flatten shouldBe (Seq(1))
  }

  it should "return successful response for several stage" in {

    val stages: List[ExecutionStage] = ExecutionStage("success", Some(signature("success"))) ::
      ExecutionStage("success", Some(signature("success"))) ::
      ExecutionStage("success", Some(signature("success"))) :: Nil
    val result = resultList(stages)
    result.size shouldBe (stages.size)
    val success = result.filter(_.requestOrError.isRequest)
    success.size shouldBe (stages.size)
    result.map(_.getRequest.inputs.values.map(_.versionNumber)).flatten shouldBe (Seq(3, 2, 1))
  }


  it should "return exception response for one stage" in {

    val stages: List[ExecutionStage] = ExecutionStage("failure", Some(signature("failure"))) :: Nil
    val result = resultList(stages)
    result.size shouldBe (stages.size)
    result.head.requestOrError.isError shouldBe (true)
  }

  it should "return exception response for several stage" in {

    val stages: List[ExecutionStage] = ExecutionStage("key1", Some(signature("success"))) ::
      ExecutionStage("failure", Some(signature("failure"))) ::
      ExecutionStage("key3", Some(signature("success"))) :: Nil
    val result = resultList(stages)
    result.size shouldBe (stages.size)
    result.map(_.requestOrError.isError) shouldBe (Seq(true, true, false))
  }

  def resultList(stages: List[ExecutionStage]): List[KafkaServingMessage] = {
    val predictor = new PredictServiceStub(stages)
    val request = new PredictRequest(inputs = input)
    val message = KafkaServingMessage()
      .withRequest(request)
      .withMeta(KafkaMessageMeta())

    val response = predictor.predictByGraph(message, app(stages))
    Await.result(Future.sequence(response), 1 second)
  }

  class PredictServiceStub(stages: Seq[ExecutionStage]) extends PredictService {

    override def fetchPredict(in: PredictRequest)(stage: String): Future[PredictResponse] = in.modelSpec.get.signatureName match {
      case "failure" => Future.failed(new RuntimeException("Some failure"))
      case _ => Future.successful(
        PredictResponse(
          in.inputs.map { case (key, value) =>
            (key, value.withVersionNumber(value.versionNumber + 1))
          }
        )
      )
    }

  }

}


