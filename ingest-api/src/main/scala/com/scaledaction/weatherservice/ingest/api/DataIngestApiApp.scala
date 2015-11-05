package com.scaledaction.weatherservice.ingest.api

import akka.actor.{ ActorSystem, Props }
import com.scaledaction.core.akka.HttpServerApp

object DataIngestApiApp extends App with HttpServerApp {

  implicit val system = ActorSystem("data-ingest")

  val kafkaProducer = system.actorOf(Props(new KafkaProducerActor))

  val route = new DataIngestApi(kafkaProducer).route

  startServer(route)
}