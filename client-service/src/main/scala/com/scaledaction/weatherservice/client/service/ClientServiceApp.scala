package com.scaledaction.weatherservice.client.service

import akka.actor.{ ActorSystem, Props }
import com.datastax.killrweather.{ PrecipitationActor, TemperatureActor, WeatherStationActor }
import com.scaledaction.core.akka.HttpServerApp
import com.scaledaction.core.cassandra.{ CassandraConfig, HasCassandraConfig }
import org.apache.spark.Logging
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.streaming.{ StreamingContext, Seconds }

object ClientServiceApp extends HttpServerApp with HasCassandraConfig with Logging {

  def main(args: Array[String]) {

    val cassandraConfig = getCassandraConfig

    //TODO - convert to using a plain SparkContext 
    // 1. if SparkContext can be used with Spark SQL
    // 2. by placing sc.stop() in an onTermination block
    //val ssc = getActiveOrCreateStreamingContext(cassandraConfig)
    val sc = getActiveOrCreateSparkContext(cassandraConfig)

    implicit val system = ActorSystem("client-service")

    val precipitation = system.actorOf(Props(new PrecipitationActor(sc, cassandraConfig)), "precipitation")
    val temperature = system.actorOf(Props(new TemperatureActor(sc, cassandraConfig)), "temperature")
    val weatherStation = system.actorOf(Props(new WeatherStationActor(sc, cassandraConfig)), "weather-station")

    val service = system.actorOf(Props(new ClientService(precipitation, temperature, weatherStation)), "client-service")

    startServer(service)

    //TODO - switch to SparkContext and add shutdown hook
    //ssc.start()
    //ssc.awaitTermination()

    //TODO - Is this already a method or built into SparkContext?
    sys addShutdownHook {
      println("Shutdown hook caught.")
      sc.stop
      println("Done shutting down.")
    }
  }

  //TODO - Create a com.scaledaction.core.spark.SparkUtils object for the following method
  def getActiveOrCreateSparkContext(cassandraConfig: CassandraConfig): SparkContext = {

    //TODO - Need to add SparkConfig and replace the hard-coded "setMaster" value
    val conf = new SparkConf()
      .set("spark.cassandra.connection.host", cassandraConfig.seednodes)
      .setMaster("local[3]")
      .setAppName("WeatherServiceClientService")

    SparkContext.getOrCreate(conf)
  }

  //TODO - Create a com.scaledaction.core.spark.SparkUtils object for the following method
  def getActiveOrCreateStreamingContext(cassandraConfig: CassandraConfig): StreamingContext = {

    //TODO - Need to add SparkConfig and replace the hard-coded "setMaster" value
    val conf = new SparkConf()
      .set("spark.cassandra.connection.host", cassandraConfig.seednodes)
      .setMaster("local[3]")
      .setAppName("WeatherServiceClientService")

    val sc = SparkContext.getOrCreate(conf)

    def createStreamingContext(): StreamingContext = {
      @transient val newSsc = new StreamingContext(sc, Seconds(2))
      logInfo(s"Creating new StreamingContext $newSsc")

      newSsc
    }
    StreamingContext.getActiveOrCreate(createStreamingContext)
  }
}
