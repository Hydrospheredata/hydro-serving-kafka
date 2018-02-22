package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.kafka.grpc.PredictionGrpcApi
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.predict.Application
import io.hydrosphere.serving.kafka.stream.{AppStreamer, Producer, Streamer}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.scalatest.{BeforeAndAfter, FlatSpec, GivenWhenThen, Matchers}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PredictionGrpcApiSpec
  extends FlatSpec
  with MockitoSugar
  with GivenWhenThen
  with BeforeAndAfter
  with Matchers  {

  var publishService:PredictionGrpcApi = _
  var streamer: AppStreamer[Array[Byte], KafkaServingMessage] = mock[AppStreamer[Array[Byte], KafkaServingMessage]]
  var producer:Producer[Array[Byte], KafkaServingMessage] = mock[Producer[Array[Byte], KafkaServingMessage]]

  before{
    publishService = new PredictionGrpcApi()(streamer, producer)
  }

  def app(name:String, inTopic:Option[String]) = Application(1, name, inTopic, None, None, None, None)

  "PredictionGrpcApiSpec" should "accept and publish valid messages for existing applications" in {

    When("Application for message found")
    when(streamer.itemsById(eqTo("existingApp")))
      .thenReturn(Set(app("existingApp", Some("in"))))

    And("producer published successfully")
    when(producer.send(eqTo("in"), any[KafkaServingMessage]))
      .thenReturn(Future.successful(recordMetadata))


    val futureResponce = publishService.predict(PredictRequest(
      modelSpec = Some(ModelSpec(name = "existingApp"))
    ))

    Then("we should get empty responce")
    val response = Await.result(futureResponce, 1 second)
    response.outputs.isEmpty shouldBe true
  }

  it should "accept and publish valid messages for several existing applications" in {

    When("Application for message found")
    when(streamer.itemsById(eqTo("existingApp")))
      .thenReturn(Set(app("existingApp",Some("in")), app("existingApp",Some("in2"))))

    And("producer published successfully")
    when(producer.send(eqTo("in"), any[KafkaServingMessage]))
      .thenReturn(Future.successful(recordMetadata))

    when(producer.send(eqTo("in2"), any[KafkaServingMessage]))
      .thenReturn(Future.successful(recordMetadata))


    val futureResponce = publishService.predict(PredictRequest(
      modelSpec = Some(ModelSpec(name = "existingApp"))
    ))

    Then("we should get empty responce")
    val response = Await.result(futureResponce, 1 second)
    response.outputs.isEmpty shouldBe true
  }

  it should "return error if topic for application couldn't be found" in {

    When("Application for message not found")
    when(streamer.itemsById(eqTo("existingApp")))
      .thenReturn(Set[Application]())

    val futureResponce = publishService.predict(PredictRequest(
      modelSpec = Some(ModelSpec(name = "existingApp"))
    ))

    Then("we should get not found exception")
    val thrown = intercept[IllegalArgumentException] {
      Await.result(futureResponce, 1 second)
    }
    assert(thrown.getMessage.startsWith("there are no configured in topics for application:"))
  }

  it should "return error if topic found but message is not published" in {

    When("Application for message found")
    when(streamer.itemsById(eqTo("existingApp")))
      .thenReturn(Set(app("existingApp", Some("in"))))

    And("producer published successfully")
    when(producer.send(eqTo("in"), any[KafkaServingMessage]))
      .thenReturn(Future.failed(new RuntimeException("oops")))

    val futureResponce = publishService.predict(PredictRequest(
      modelSpec = Some(ModelSpec(name = "existingApp"))
    ))

    Then("we should get not found exception")
    val thrown = intercept[RuntimeException] {
      Await.result(futureResponce, 1 second)
    }
    assert(thrown.getMessage === "oops")
  }

  def recordMetadata = new RecordMetadata(new TopicPartition("topic", 0), 1, 2, 10000, 10000, 1, 1)


}
