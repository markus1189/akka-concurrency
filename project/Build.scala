import sbt._
import sbt.Keys._

object AkkaBookBuild extends Build {
  lazy val akkaBook = Project(
    id = "akka-book",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Akka Book",
      organization := "zzz.akka",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.1",
      scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq( "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"
                                 , "com.typesafe.akka" %% "akka-actor" % "2.1.0" 
                                 , "com.typesafe.akka" %% "akka-testkit" % "2.1.0" % "test"
                                 , "org.scalaz" %% "scalaz-core" % "7.0.0"
                                 )
    )
  )
}

