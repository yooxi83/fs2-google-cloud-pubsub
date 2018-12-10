package com.github.hyjay.pubsub

import java.util.UUID

import scala.io.Source
import scala.util.Try

trait PubsubTestContext {

  val projectId: String = Try {
    import io.circe.parser._

    val fileLocation = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    val json = parse(Source.fromFile(fileLocation).mkString).right.get
    json.hcursor.downField("project_id").as[String].right.get
  }.getOrElse(throw new RuntimeException("GOOGLE_APPLICATION_CREDENTIALS is missing or invalid"))

  val topic: String = s"test-topic-${UUID.randomUUID().toString}"
  val subscription: String = s"$topic-subscription"
}
