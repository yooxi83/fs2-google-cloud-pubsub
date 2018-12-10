name := "fs2-google-cloud-pubsub"

version := "0.1.0-SNAPSHOT"

organization := "com.github.hyjay"
scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-pubsub" % "1.55.0",
  "co.fs2" %% "fs2-io" % "1.0.2",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "io.circe" %% "circe-parser" % "0.10.1" % Test
)
