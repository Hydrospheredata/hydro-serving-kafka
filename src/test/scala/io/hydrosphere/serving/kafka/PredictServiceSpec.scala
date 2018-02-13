package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.kafka.predict.PredictService
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class PredictServiceSpec
  extends FlatSpec
  with Matchers {

  implicit val ececutionContext = global

  def signature(key:String) = ModelSignature(key)

  val input = Map[String, TensorProto](("key", TensorProto()))

  "PredictService" should "return successful response for one stage" in {

    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("success"))) :: Nil
    val predictor = new PredictServiceStub(stages)

    val response = predictor.predictByGraph("SomeApp", new PredictRequest(
      inputs = input
    ), ExecutionGraph(stages))

    val result = predictor.report("traceId", response)

    Await.result(result, 1 second).requestOrError.isRequest shouldBe true
    Await.result(result, 1 second).requestOrError.request.get.inputs shouldBe input
  }

  "PredictService" should "return successful response for several stage" in {

    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("success"))) ::
      ExecutionStage("key2", Some(signature("success"))) ::
      ExecutionStage("key3", Some(signature("success"))) :: Nil
    val predictor = new PredictServiceStub(stages)

    val response = predictor.predictByGraph("SomeApp", new PredictRequest(
      inputs = input
    ), ExecutionGraph(stages))

    val result = predictor.report("traceId", response)

    Await.result(result, 1 second).requestOrError.isRequest shouldBe true
    Await.result(result, 1 second).requestOrError.request.get.inputs shouldBe input
  }

  "PredictService" should "return exception response for one stage" in {

    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("failure"))) :: Nil
    val predictor = new PredictServiceStub(stages)

    val response = predictor.predictByGraph("SomeApp", new PredictRequest(
      inputs = input
    ), ExecutionGraph(stages))

    val result = predictor.report("traceId", response)


    Await.result(result, 1 second).requestOrError.isError shouldBe true
  }

  "PredictService" should "return exception response for several stage" in {

    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("success"))) ::
      ExecutionStage("key2", Some(signature("failure"))) ::
      ExecutionStage("key3", Some(signature("success"))) :: Nil

    val predictor = new PredictServiceStub(stages)

    val response = predictor.predictByGraph("SomeApp", new PredictRequest(
      inputs = input
    ), ExecutionGraph(stages))

    val result = predictor.report("traceId", response)

    Await.result(result, 1 second).requestOrError.isError shouldBe true
  }

  class PredictServiceStub(stages: Seq[ExecutionStage]) extends PredictService {

    override def fetchPredict(in: PredictRequest): Future[PredictResponse] = in.modelSpec.get.signatureName match {
      case "failure" => Future.failed(new RuntimeException("Some failure"))
      case _ => Future.successful(
        PredictResponse(
          in.inputs
        )
      )
    }

  }

}


