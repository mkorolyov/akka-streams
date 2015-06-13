import sbt.Keys._
import sbt._

object AppBuild extends Build with Dependencies with BuildUtils {
  lazy val root = Project("akka-streams", file("."))
    .settings(libraryDependencies ++= akka ++ logs ++ reactiveMongo ++ scalaz ++Seq(config, nscalaTime))
    .settings(
      Seq(
        organization := "mkorolyov",
        scalaVersion := "2.11.6",
//        resolvers := depResolvers,
        libraryDependencies ~= withSources
      ): _*
    )

}

trait Dependencies extends Resolvers with Versions {
  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVer,
    "com.typesafe.akka" %% "akka-kernel" % akkaVer,
    /*"com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-RC3",
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-RC3",*/
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "1.0-RC3"
  )

  val config = "com.typesafe" % "config" % "1.0.0"

  val logs = Seq(
    "ch.qos.logback" % "logback-classic" % logbackVer,
    "ch.qos.logback" % "logback-core" % logbackVer,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVer
  )

  val scalazCore = "org.scalaz" %% "scalaz-core" % scalazVersion
  val scalaz = Seq(scalazCore)

  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "1.8.0"

  val reactiveMongo = Seq(
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion,
    "com.typesafe.play" %% "play-streams-experimental" % "2.4.0-M2",
    "org.reactivemongo" %% "reactivemongo-extensions-bson" % reactiveMongoExtensionVersion,
    "org.reactivemongo" %% "reactivemongo-extensions-json" % reactiveMongoExtensionVersion
  )
}

trait Versions {
  val akkaVer = "2.3.11"
  val reactiveMongoVersion = "0.10.5.0.akka23"
  val reactiveMongoExtensionVersion = "0.10.5.0.0.akka23"
  val logbackVer = "1.1.2"
  val scalazVersion = "7.1.2"
}

trait Resolvers {
  val typesafeReleaseRepo = "Typesafe Release Repository" at
    "http://repo.typesafe.com/typesafe/releases/"

  val depResolvers = Seq(
    typesafeReleaseRepo
  )
}

trait BuildUtils {
  def withSources(allDependencies: Seq[ModuleID]): Seq[ModuleID] = {
    val (withoutSourcesDeps, withSourcesDeps) =
      allDependencies.partition(_.name.contains("play"))
    withSourcesDeps.map(_ withSources() withJavadoc()) ++ withoutSourcesDeps
  }
}