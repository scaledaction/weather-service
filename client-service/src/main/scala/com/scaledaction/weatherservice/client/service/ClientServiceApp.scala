package com.scaledaction.weatherservice.client.service

import akka.actor.{ ActorSystem, Props }
import com.scaledaction.core.akka.{ HttpServerApp, HasHttpServerConfig }
import com.datastax.killrweather.PrecipitationActor

object ClientServiceApp extends App with HttpServerApp with HasHttpServerConfig {

  implicit val system = ActorSystem("data-ingest")

  val producer = system.actorOf(Props(new PrecipitationActor))
  //
  //val route = new ClientService(producer).route
  //val route = new ClientService().route
  val route = system.actorOf(Props(new ClientService(producer)))

  val httpConfig = getHttpServerConfig

  startServer(httpConfig)(route)
}