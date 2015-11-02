package com.scaledaction.weatherservice.ingest.api

import akka.actor.{ ActorSystem, Props }
import com.scaledaction.core.akka.{ HttpServerApp, HasHttpServerConfig }

object DataIngestApiApp extends App with HttpServerApp with HasHttpServerConfig {

  implicit val system = ActorSystem("data-ingest")

  val producer = system.actorOf(Props(new KafkaProducerActor))

  val route = new DataIngestApi(producer).route

  val httpConfig = getHttpServerConfig

  startServer(interface = httpConfig.host, port = httpConfig.port)(route)
}