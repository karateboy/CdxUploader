name := """CdxUploader"""

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "org.scalikejdbc" %% "scalikejdbc"       % "2.4.1",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.4.1",
  "ch.qos.logback"  %  "logback-classic"   % "1.1.7",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0",
  "jline" % "jline" % "2.14.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test")

scalacOptions += "-feature"
scalacOptions += "-deprecation"

fork in run := true