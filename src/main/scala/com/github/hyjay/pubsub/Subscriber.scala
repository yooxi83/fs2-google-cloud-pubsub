package com.github.hyjay.pubsub

import cats.effect.IO
import com.google.api.core.ApiService
import com.google.api.gax.batching.FlowControlSettings
import com.google.cloud.pubsub.v1.{AckReplyConsumer, MessageReceiver, Subscriber => JavaSubscriber}
import com.google.common.util.concurrent.MoreExecutors
import com.google.pubsub.v1.{ProjectSubscriptionName, PubsubMessage}

/**
  * Pub/Sub subscriber.
  */
trait Subscriber {

  /**
    * Starts pulling messages from a subscription within the object. It returns an IO instance that ends
    * when pulling terminates due to any errors or process termination.
    *
    * Please note that Subscriber acknowledges parsing failed messages and messages that
    * {@param run} gave a success for. But it nacks messages that {@param run} gave a fail for.
    *
    * @param parser A parser function for Pub/Sub payloads. It returns Some for successful parsed, otherwise None.
    * @param run An IO operation to handle messages from the parser.
    * @tparam A A type of message by the parser.
    */
  def subscribe[A](parser: Array[Byte] => Option[A])(run: A => IO[Unit]): IO[Unit]
}

object Subscriber {

  /**
    * Creates a Subscriber instance.
    *
    * @param projectId A Google cloud project id.
    * @param subscription A subscription name.
    * @return A Subscriber instance.
    */
  def create(projectId: String, subscription: String): Subscriber =
    new Subscriber {
      def subscribe[A](parser: Array[Byte] => Option[A])(run: A => IO[Unit]): IO[Unit] = {
        val messageReceiver = new MessageReceiver {
          override def receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer): Unit = {
            parser(message.getData.toByteArray) match {
              case Some(a) =>
                run(a).unsafeRunAsync {
                  case Right(_) => consumer.ack()
                  case Left(_) => consumer.nack()
                }
              case None =>
                consumer.ack()
            }
          }
        }

        val subscriptionName = ProjectSubscriptionName.of(projectId, subscription)
        val flowControlSettings = FlowControlSettings.newBuilder
          .setMaxOutstandingRequestBytes(Runtime.getRuntime.maxMemory * 1 / 100L)
          .build
        val subscriber = JavaSubscriber.newBuilder(subscriptionName, messageReceiver)
          .setFlowControlSettings(flowControlSettings)
          .build

        val awaitFailure = IO.async[Unit] { cb =>
          subscriber.addListener(new ApiService.Listener() {
            override def failed(from: ApiService.State, failure: Throwable): Unit = {
              cb(Left(failure))
            }
          }, MoreExecutors.directExecutor)
        }

        for {
          _ <- IO { subscriber.startAsync() }
          _ <- awaitFailure
        } yield ()
      }
    }
}
