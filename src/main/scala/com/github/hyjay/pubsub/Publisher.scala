package com.github.hyjay.pubsub

import cats.effect.IO
import com.google.cloud.pubsub.v1.{Publisher => JavaPublisher}
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ProjectTopicName, PubsubMessage}

/**
  * Pub/Sub publisher.
  */
trait Publisher {

  /**
    * Publishes a Pub/Sub message with {@param bytes} as payload, to a topic within the object.
    * It returns an IO instance of the corresponding Pub/Sub message id for the published message.
    *
    * @param bytes The payload for a Pub/Sub message
    */
  def publish(bytes: Array[Byte]): IO[String]
}

object Publisher {

  /**
    * Creates a publisher. Please note that it returns an IO instance that ensures {@param topic} is created.
    *
    * @param projectId A Google cloud project id.
    * @param topic A topic name.
    */
  def create(projectId: String, topic: String): IO[Publisher] =
    for {
      _ <- TopicFunctions(projectId).createTopic(topic)
      p <- IO { JavaPublisher.newBuilder(ProjectTopicName.of(projectId, topic)).build }
    } yield new Publisher {
      override def publish(bytes: Array[Byte]): IO[String] = {
        import ApiFutureToScala._
        IO.fromFuture(IO {
          p.publish(
            PubsubMessage.newBuilder()
              .setData(ByteString.copyFrom(bytes))
              .build()
          ).toScala
        })
      }
    }
}
