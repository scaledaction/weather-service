package com.scaledaction.weatherservice.client.service

import akka.actor.{ ActorSystem, Props }
import com.scaledaction.core.akka.HttpServerApp
import com.datastax.killrweather.PrecipitationActor

object ClientServiceApp extends App with HttpServerApp {

  implicit val system = ActorSystem("client-service")

  val precipitation = system.actorOf(Props(new PrecipitationActor))
  //val temperature = system.actorOf(Props(new TemperatureActor))
  //val weatherStation = system.actorOf(Props(new WeatherStationActor))

  //val service = system.actorOf(Props(new ClientService(precipitation, temperature, weatherStation)))
  val service = system.actorOf(Props(new ClientService(precipitation)))

  startServer(service)
}