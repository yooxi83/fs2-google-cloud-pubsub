name := "fs2-google-cloud-pubsub"

version := "0.1.0"
scalaVersion := "2.12.8"

organization := "com.github.hyjay"
homepage := Some(url("https://github.com/hyjay/fs2-google-cloud-pubsub"))
developers := List(Developer(
  "hyjay",
  "Jay Kim",
  "hyeonjay.kim@gmail.com",
  url("https://github.com/hyjay")
))
licenses += ("MIT", url("https://github.com/hyjay/fs2-google-cloud-pubsub/blob/master/LICENSE"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/hyjay/fs2-google-cloud-pubsub"),
    "https://github.com/hyjay/fs2-google-cloud-pubsub.git"
  )
)
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
publishMavenStyle := true

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-pubsub" % "1.55.0",
  "co.fs2" %% "fs2-io" % "1.0.2",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "io.circe" %% "circe-parser" % "0.10.1" % Test
)
