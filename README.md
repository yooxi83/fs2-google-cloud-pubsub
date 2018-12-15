# fs2-google-cloud-pubsub
[![Build Status](https://travis-ci.org/hyjay/fs2-google-cloud-pubsub.svg?branch=master)](https://travis-ci.org/hyjay/fs2-google-cloud-pubsub)  

A developer-first Google cloud Pub/Sub client in Scala, based on [the official Google cloud Java client library](https://github.com/googleapis/google-cloud-java/tree/master/google-cloud-clients/google-cloud-pubsub).

## Getting Started
#### Publishing messages
With the library you can publish messages to a topic with Publisher which automatically will create the topic for you.
```scala
import com.github.hyjay.pubsub.Publisher

val io = for {
  // Create a publisher and the returned IO will idempotently create the topic for you. 
  publisher <- Publisher.create("YOUR_PROJECT_ID", "YOUR_TOPIC")
  messageId <- publisher.publish("Hello, world!".getBytes())
} yield ()

io.unsafeRunSync()
```

#### Creating a subscription
With the library you can create subscriptions as simple as it can be.
```scala
import com.github.hyjay.pubsub.SubscriptionFunctions

val io = SubscriptionFunctions("YOUR_PROJECT_ID").createSubscription("YOUR_TOPIC", "YOUR_SUBSCRIPTION")

io.unsafeRunSync()
```

#### Pulling messages
With the library you can pull messages from a subscription as simple as it can be.
```scala
import scala.util.Try
import cats.effect.IO
import com.github.hyjay.pubsub.Subscriber

// Return None for parsing failed messages
def parser(payload: Array[Byte]): Option[String] = Try(new String(payload)).toOption

// Print out parsed messages as strings
def run(message: String): IO[Unit] = IO { println(message) }
 
// Subscriber acknowledges parsing failed messages or messages handled by `run` without an error
// It nacks messages that are parsed but having an error while handling
val printMessages = Subscriber.create("YOUR_PROJECT_ID", "YOUR_SUBSCRIPTION").subscribe(parser)(run)

// The IO ends when pulling stop by any errors.
printMessages.unsafeRunSync()
```

## Testing
If you want to run the test code in this library, set GOOGLE_APPLICATION_CREDENTIALS envvar to a file location of
google application credential json file.  
Please note that the test code calls actual Google cloud Pub/Sub APIs.

## License
MIT - See LICENSE for more information.