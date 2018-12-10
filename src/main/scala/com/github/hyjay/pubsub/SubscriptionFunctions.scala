package com.github.hyjay.pubsub

import cats.effect.IO
import com.google.api.gax.rpc.{AlreadyExistsException, NotFoundException}
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.pubsub.v1.{ProjectSubscriptionName, ProjectTopicName, PushConfig}
import fs2.Stream

/**
  * A set of functions that administrates Pub/Sub subscriptions.
  */
trait SubscriptionFunctions {

  /**
    * Creates a subscription idempotently. It returns false if the subscription already exists and do nothing.
    * Otherwise, returns true.
    *
    * Please Note that it also creates a topic if the topic does not exist.
    *
    * @param topic A topic name for the subscription.
    * @param subscription A subscription name.
    * @param ackDeadlineLatency An acknowledge deadline latency in seconds.
    */
  def createSubscription(topic: String,
                         subscription: String,
                         ackDeadlineLatency: Int = 80): IO[Boolean]

  /**
    * Deletes a subscription idempotently. It returns false if the subscription does not exist.
    * Otherwise, returns true.
    *
    * @param subscription A subscription name.
    */
  def deleteSubscription(subscription: String): IO[Boolean]
}

object SubscriptionFunctions {

  import cats.implicits._

  /**
    * Creates a SubscriptionFunctions instance for {@param projectId}.
    *
    * @param projectId Google cloud project id.
    */
  def apply(projectId: String): SubscriptionFunctions =
    new SubscriptionFunctions {

      private def subscriptionAdminClient: Stream[IO, SubscriptionAdminClient] =
        Stream.bracket(IO { SubscriptionAdminClient.create() })(client => IO { client.close() })

      def createSubscription(topic: String, subscription: String, ackDeadlineLatency: Int = 80): IO[Boolean] = {
        val run = for {
          client <- subscriptionAdminClient
          _ <- Stream.eval(TopicFunctions(projectId).createTopic(topic))
          _ <- Stream.eval(IO {
            client.createSubscription(
              ProjectSubscriptionName.of(projectId, subscription),
              ProjectTopicName.of(projectId, topic),
              PushConfig.getDefaultInstance,
              ackDeadlineLatency
            )
          })
        } yield ()

        run.compile.drain.map(_ => true).handleError {
          case _: AlreadyExistsException => false
        }
      }

      def deleteSubscription(subscription: String): IO[Boolean] = {
        val run = for {
          client <- subscriptionAdminClient
          _ <- Stream.eval(IO { client.deleteSubscription(ProjectSubscriptionName.of(projectId, subscription)) })
        } yield ()

        run.compile.drain.map(_ => true).handleError {
          case _: NotFoundException => false
        }
      }
    }
}
