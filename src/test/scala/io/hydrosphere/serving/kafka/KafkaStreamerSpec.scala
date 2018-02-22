package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.kafka.stream.{DoubleStream, Streamer}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, GivenWhenThen, Matchers}

import scala.collection.Set


class KafkaStreamerSpec
  extends FlatSpec
    with GivenWhenThen
    with MockitoSugar
    with BeforeAndAfter
    with Matchers {

  var streamer:TestStreamer = _
  val stream = mock[StreamFromKey[Int, Int, Any, TestItem]]

  before {
    streamer = new TestStreamer
    streamer.updateStore(stream, version => {})(Seq(TestItem("1", 1),TestItem("2", 2)), "1")
  }

  "KafkaStreamer" should "properly initiate store" in {

    Then("should find created apps")
    streamer.itemsById("1") shouldBe Set(TestItem("1", 1))
    streamer.itemsById("2") shouldBe Set(TestItem("2", 2))

    And("and opposite if it wasn't")
    streamer.itemsById("3") shouldBe Set()

  }

  it should "delete from store unregistered items" in {

    streamer.updateStore(stream, version => {})(Seq(), "2")

    Then("shouldn't find removed or uncreated apps")
    streamer.itemsById("1") shouldBe Set()
    streamer.itemsById("2") shouldBe Set()
    streamer.itemsById("3") shouldBe Set()
  }

  it should "properly update values" in {
    streamer.updateStore(stream, version => {})(Seq(TestItem("3", 33),TestItem("2", 22)), "2")

    Then("shouldn't find removed or uncreated apps")
    streamer.itemsById("1") shouldBe Set()
    streamer.itemsById("2") shouldBe Set(TestItem("2", 22))
    streamer.itemsById("3") shouldBe Set(TestItem("3", 33))
  }

  case class TestItem(name:String, id:Int)

  class TestStreamer extends Streamer[Int, Int, TestItem, String] {
    def stop():Unit = ???
    override def streamForAll[R](stream: KeyAndStream[Int,Int, TestItem] => DoubleStream[Int,R]): Unit = ???
    override def itemsById(appName:String):Set[TestItem] = store.keySet.filter(_.name == appName).toSet
    override def deleteAction(tuple:(TestItem, String)):Unit = {

    }
    protected[this] def streamFor[R](key: TestItem)(transform: KeyAndStream[Int, Int, TestItem] => DoubleStream[Int, R]) = {
      Some((key, key.name))
    }
  }

}

