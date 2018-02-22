package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaError, KafkaMessageLocation, KafkaMessageMeta, KafkaServingMessage}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import org.scalatest.{BeforeAndAfter, FlatSpec, GivenWhenThen, Matchers}
import cats.syntax.option._
import io.hydrosphere.serving.kafka.predict.Application
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.kafka.utils.KafkaMessageUtils._
import scala.concurrent.duration._


import scala.concurrent.{Await, Future}

class KafkaMessageUtilsSpec
  extends FlatSpec
    with GivenWhenThen
    with BeforeAndAfter
    with Matchers {

  implicit val ctx = scala.concurrent.ExecutionContext.global
  implicit val future = cats.implicits.catsStdInstancesForFuture

  "KafkaMessageUtils" should "enrich message with app data" in{
    When("change app Id")
    val initial = initialSuccessMessage
    val forward = initial.forApplicaton(application("newAppId"))

    Then("app id should be changed")
    forward shouldNot be(initial)
    forward.meta.get.applicationId shouldBe "newAppId"
    forward.getRequest.modelSpec.get.name shouldBe "newAppId"

    And("other fields should be the same")
    val backward = forward.forApplicaton(application("oldAppId"))
    backward shouldBe initial

  }

  it should "enrich message with kafka data" in {
    val initial = initialSuccessMessage

    When("enriched with kafka data")
    val withKafkaData = initial.withKafkaData("topic", 2, 100, "consumerId")

    Then("all fields should be set")
    val location = withKafkaData.meta.get.location.get
    location.sourceTopic shouldBe "topic"
    location.offset shouldBe 100
    location.partition shouldBe 2
    location.consumerId shouldBe "consumerId"

    And("other fields should be the same")
    val backward = withKafkaData.withMeta(
      withKafkaData.meta.get.withLocation(initial.meta.get.location.get)
    )

    backward shouldBe initial

  }

  it should "transform successful message with valid response and return success message" in {

    When("transform message with successfull transformation")
    val transformedF = initialSuccessMessage.applyIfRequest[Future](request => Future.successful(
      PredictResponse(Map("Success" -> TensorProto()))
    ))

    Then("we get successfull result message with changed inputs")
    val transformed = Await.result(transformedF, 2 seconds)
    transformed.requestOrError.isRequest shouldBe true

    And("message should be transformed properly")
    transformed.requestOrError.request.get.inputs.get("Success").isDefined shouldBe true
    transformed.requestOrError.request.get.inputs.size shouldBe 1
  }

  it should "transform unsuccessful message with valid response and return error message" in{
    When("transform message with successfull transformation")
    val transformedF = initialErrorMessage("error").applyIfRequest[Future](request => Future.successful(
      PredictResponse(Map("Success" -> TensorProto()))
    ))

    Then("we get failure result message")
    val transformed = Await.result(transformedF, 2 seconds)
    transformed.requestOrError.isRequest shouldBe false

    And("exception message should be the same")
    transformed.requestOrError.error.get.errorMessage shouldBe "error"
  }

  it should "transform successful message with invalid response and return error message" in{
    When("transform message with successfull transformation")
    val transformedF = initialSuccessMessage.applyIfRequest[Future](request =>
      Future.failed(new RuntimeException("runtime exception"))
    ) recover { case e: Exception =>
      initialSuccessMessage.withError(
        KafkaError(errorMessage = e.getMessage, lastKnownRequest = initialSuccessMessage.requestOrError.request)
      )
    }

    Then("we get failure result message")
    val transformed = Await.result(transformedF, 2 seconds)
    transformed.requestOrError.isRequest shouldBe false

    And("exception message should be the same")
    transformed.requestOrError.error.get.errorMessage shouldBe "runtime exception"
  }

  def initialErrorMessage(message:String) = initialEmptyMessage.withRequestOrError {
    RequestOrError.Error{
      KafkaError().withErrorMessage(message)
        .withLastKnownRequest(request)
    }
  }

  def application(id: String) = Application( 1, id, none, none, none, none, none)

  def initialSuccessMessage = initialEmptyMessage
    .withRequest(request)

  def request = PredictRequest()
    .withModelSpec(ModelSpec("oldAppId", 1l.some))
    .withInputs(Map("a" -> TensorProto()))

  def initialEmptyMessage = KafkaServingMessage()
    .withMeta(KafkaMessageMeta()
      .withApplicationId("oldAppId")
        .withTraceId("traceID")
        .withLocation(KafkaMessageLocation()
          .withConsumerId("consumerId")
          .withOffset(100)
          .withPartition(3)
          .withSourceTopic("source"))
        .withStageId("stageID"))


}
