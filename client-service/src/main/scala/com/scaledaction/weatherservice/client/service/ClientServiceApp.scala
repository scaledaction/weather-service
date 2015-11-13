package com.scaledaction.weatherservice.client.service

import akka.actor.{ ActorSystem, Props }
import com.datastax.killrweather.{ PrecipitationActor, TemperatureActor, WeatherStationActor }
import com.scaledaction.core.akka.HttpServerApp
import com.scaledaction.core.cassandra.HasCassandraConfig
import com.scaledaction.core.spark.HasSparkConfig
import com.scaledaction.core.spark.SparkUtils

object ClientServiceApp extends App with HttpServerApp with HasCassandraConfig with HasSparkConfig {

  val cassandraConfig = getCassandraConfig
  
   val sparkConfig = getSparkConfig 

  //TODO - Need to add ApplicationConfig and replace the hard-coded "sparkAppName" value with application.app-name
  val sc = SparkUtils.getActiveOrCreateSparkContext(cassandraConfig, sparkConfig.master, "WeatherService")

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
