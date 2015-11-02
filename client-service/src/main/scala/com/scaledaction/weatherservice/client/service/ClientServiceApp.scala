package com.scaledaction.weatherservice.client.service

import akka.actor.{ ActorSystem, Props }
import com.scaledaction.core.akka.{ HttpServerApp, HasHttpServerConfig }

object ClientServiceApp extends App with HttpServerApp with HasHttpServerConfig {

  implicit val system = ActorSystem("data-ingest")

  //val producer = system.actorOf(Props(new KafkaProducerActor))
  //
  //val route = new ClientService(producer).route
  val route = new ClientService().route

  val httpConfig = getHttpServerConfig

  startServer(interface = httpConfig.host, port = httpConfig.port)(route)
}