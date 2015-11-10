package com.scaledaction.weatherservice.client.service

import akka.actor.{ ActorSystem, Props }
import com.datastax.killrweather.{ PrecipitationActor, TemperatureActor, WeatherStationActor }
import com.scaledaction.core.akka.HttpServerApp
import com.scaledaction.core.cassandra.{ CassandraConfig, HasCassandraConfig }
import com.scaledaction.core.spark.SparkUtils

object ClientServiceApp extends App with HttpServerApp with HasCassandraConfig {

  val cassandraConfig = getCassandraConfig

  //TODO - Need to add SparkConfig and replace the hard-coded "sparkMaster" and "sparkAppName" value
  val sc = SparkUtils.getActiveOrCreateSparkContext(cassandraConfig, "local[3]", "WeatherService")

  implicit val system = ActorSystem("client-service")

  val precipitation = system.actorOf(Props(new PrecipitationActor(sc, cassandraConfig)), "precipitation")
  val temperature = system.actorOf(Props(new TemperatureActor(sc, cassandraConfig)), "temperature")
  val weatherStation = system.actorOf(Props(new WeatherStationActor(sc, cassandraConfig)), "weather-station")

  val service = system.actorOf(Props(new ClientService(precipitation, temperature, weatherStation)), "client-service")

  startServer(service)

  sys addShutdownHook {
    sc.stop
  }
}
