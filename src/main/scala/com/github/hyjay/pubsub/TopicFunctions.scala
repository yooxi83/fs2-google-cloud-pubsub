package com.github.hyjay.pubsub

import cats.effect.IO
import com.google.api.gax.rpc.{AlreadyExistsException, NotFoundException}
import com.google.cloud.pubsub.v1.TopicAdminClient
import com.google.pubsub.v1.ProjectTopicName
import fs2.Stream

/**
  * A set of functions that administrates Pub/Sub topics.
  */
trait TopicFunctions {

  /**
    * Creates a topic idempotently. It returns false if the topic already exists. Otherwise, returns true.
    *
    * @param topic A Pub/Sub topic name.
    */
  def createTopic(topic: String): IO[Boolean]

  /**
    * Deletes a topic. It returns false if the topic does not exist. Otherwise, returns true.
    *
    * @param topic A Pub/Sub topic name.
    */
  def deleteTopic(topic: String): IO[Boolean]
}

object TopicFunctions {

  import cats.implicits._

  /**
    * Creates a TopicFunctions instance about {@param projectId}.
    *
    * @param projectId Google cloud project id.
    */
  def apply(projectId: String): TopicFunctions =
    new TopicFunctions {

      private def topicAdminClient: Stream[IO, TopicAdminClient] =
        Stream.bracket(IO { TopicAdminClient.create() })(client => IO { client.close() })

      override def createTopic(topic: String): IO[Boolean] = {
        val run = for {
          client <- topicAdminClient
          _ <- Stream.eval(IO { client.createTopic(ProjectTopicName.of(projectId, topic)) })
        } yield ()

        run.compile.drain.map(_ => true).handleError {
          case _: AlreadyExistsException => false
        }
      }

      override def deleteTopic(topic: String): IO[Boolean] = {
        val run = for {
          client <- topicAdminClient
          _ <- Stream.eval(IO { client.deleteTopic(ProjectTopicName.of(projectId, topic)) })
        } yield ()

        run.compile.drain.map(_ => true).handleError {
          case _: NotFoundException => false
        }
      }
    }
}
