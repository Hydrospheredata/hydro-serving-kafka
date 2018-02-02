package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.kafka.services.PredictService
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class PredictServiceSpec
  extends FlatSpec
  with Matchers {

  implicit val ecevutionContext = global

  def signature(key:String) = ModelSignature(key)

  val input = Map[String, TensorProto](("key", TensorProto()))

//  "PredictService" should "return successful response for one stage" in {
//
//    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("success"))) :: Nil
//    val predictor = new PredictServiceStub(stages)
//
//    val response = predictor.predictByGraph(new PredictRequest(
//      Some(ModelSpec()),
//      input
//    ))
//
//    Await.result(response, 1 second).outputs shouldBe input
//  }
//
//  "PredictService" should "return successful response for several stage" in {
//
//    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("success"))) ::
//      ExecutionStage("key2", Some(signature("success"))) ::
//      ExecutionStage("key3", Some(signature("success"))) :: Nil
//    val predictor = new PredictServiceStub(stages)
//
//    val response = predictor.predictByGraph(new PredictRequest(
//      Some(ModelSpec()),
//      input
//    ))
//
//    Await.result(response, 1 second).outputs shouldBe input
//  }
//
//  "PredictService" should "return exception response for one stage" in {
//
//    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("failure"))) :: Nil
//    val predictor = new PredictServiceStub(stages)
//
//    val response = predictor.predictByGraph(new PredictRequest(
//      Some(ModelSpec()),
//      input
//    ))
//
//    val withFallBack = response
//      .map(Success(_))
//      .recover { case e:Exception => Failure[PredictResponse](e) }
//
//    Await.result(withFallBack, 1 second).isFailure shouldBe true
//  }

  "PredictService" should "return exception response for several stage" in {

    val stages:Seq[ExecutionStage] = ExecutionStage("key1", Some(signature("success"))) ::
      ExecutionStage("key2", Some(signature("failure"))) ::
      ExecutionStage("key3", Some(signature("success"))) :: Nil

    val predictor = new PredictServiceStub(stages)

    val response = predictor.predictByGraph(new PredictRequest(
      Some(ModelSpec()),
      input
    ))

    val withFallBack = response
      .map(Success(_))
      .recover { case e:Exception => Failure[PredictResponse](e) }

    Await.result(withFallBack, 1 second).isFailure shouldBe true
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
    override def getExecutionGraph(modelSpec: ModelSpec): ExecutionGraph = ExecutionGraph(stages)

  }

}


