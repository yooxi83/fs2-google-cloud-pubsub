package com.github.hyjay.pubsub

import java.util.UUID

import cats.effect.IO
import com.google.api.gax.rpc.NotFoundException
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.util.Try
import fs2.Stream

class PublishAndSubscribeSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  import scala.concurrent.duration._

  private val topicFunctions = TopicFunctions(projectId)
  private val subscriptionFunctions = SubscriptionFunctions(projectId)

  import topicFunctions._
  import subscriptionFunctions._

  override def beforeEach(): Unit = {
    try {
      super.beforeEach()
    } finally {
      deleteSubscription(subscription).flatMap(_ => deleteTopic(topic)).unsafeRunSync()
    }
  }

  "Publisher and subscriber" should "send and receive a Pub/Sub message" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val contextShift = IO.contextShift(global)

    val randomMessage = UUID.randomUUID().toString
    val sendAndReceive = for {
      queue <- fs2.concurrent.Queue.unbounded[IO, String]
      _ <- createSubscription(topic, subscription)
      p <- Publisher.create(projectId, topic)
      subscriber = Subscriber.create(projectId, subscription)
      _ <- p.publish(randomMessage.getBytes())
      _ <- IO { subscriber.subscribe(bytes => Try(new String(bytes)).toOption)(queue.enqueue1).unsafeRunAsync(_ => ()) }
      received <- queue.dequeue1
      _ = received shouldBe randomMessage
    } yield ()

    sendAndReceive.unsafeRunSync()
  }

  "Subscriber" should "terminate pulling if any errors occurred" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val timer = IO.timer(global)
    implicit val contextShift = IO.contextShift(global)

    val pullAndTerminate = for {
      _ <- Stream.eval(createSubscription(topic, subscription))
      // Delete a subscription after 15 seconds
      deleteSubscriptionDelay = timer.sleep(15.seconds).flatMap(_ => deleteSubscription(subscription))
      _ <- Stream.eval(IO { deleteSubscriptionDelay.unsafeRunAsync(_ => ()) })
      subscriber = Subscriber.create(projectId, subscription)
      _ <- Stream.eval(subscriber.subscribe(_ => Some(""))(_ => IO.unit))
    } yield ()

    the[NotFoundException] thrownBy pullAndTerminate.compile.drain.unsafeRunSync()
  }
}
