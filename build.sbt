name := "akka_test"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-agent" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6",
  "org.apache.spark" %% "spark-core" % "1.3.0",
  "org.specs2" %% "specs2-core" % "2.3.12"
)

