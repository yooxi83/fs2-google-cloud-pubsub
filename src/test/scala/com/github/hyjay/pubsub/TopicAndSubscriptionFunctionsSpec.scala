package com.github.hyjay.pubsub

import org.scalatest.{FlatSpec, Matchers}

class TopicAndSubscriptionFunctionsSpec extends FlatSpec with Matchers {

  private val subscriptionFunctions = SubscriptionFunctions(projectId)
  private val topicFunctions = TopicFunctions(projectId)

  import topicFunctions._
  import subscriptionFunctions._

  "TopicFunctions" should "provide functions that creates and deletes a topic" in {
    createTopic(topic).unsafeRunSync() shouldBe true
    createTopic(topic).unsafeRunSync() shouldBe false
    deleteTopic(topic).unsafeRunSync() shouldBe true
    deleteTopic(topic).unsafeRunSync() shouldBe false
  }

  "SubscriptionFunctions" should "provide functions that creates and deletes a subscription" in {
    // createSubscription will automatically create the topic of the subscription
    createSubscription(topic, subscription).unsafeRunSync() shouldBe true
    createSubscription(topic, subscription).unsafeRunSync() shouldBe false
    deleteSubscription(subscription).unsafeRunSync() shouldBe true
    deleteSubscription(subscription).unsafeRunSync() shouldBe false

    deleteTopic(topic).unsafeRunSync() shouldBe true
  }
}
