package com.scaledaction.weatherservice.ingest.api

import akka.actor.{ ActorSystem, Props }
import com.scaledaction.core.akka.{ HttpServerApp, HttpServerConfig }

object DataIngestApiApp extends App with HttpServerApp with HttpServerConfig {

  implicit val system = ActorSystem("data-ingest")

  val producer = system.actorOf(Props(new KafkaProducerActor))

  val route = new DataIngestApi(producer).route

  startServer(interface = HttpHost, port = HttpPort)(route)
}


//  val config = new AppConfig
//  import config._

  //implicit val executionContext = system.dispatcher
  //implicit val timeout = requestTimeout(config)

  //val api = system.actorOf(Props(new RestApi(timeout)), "httpInterface")
  //
  //  //  val config = ConfigFactory.load()
  //  //  val host = config.getString("http.host")
  //  //  val port = config.getInt("http.port")
  //val host = "localhost"
  //  val host = "10.0.2.71"
  //val port = 8080
  //startServer(interface = host, port = port)(route)

