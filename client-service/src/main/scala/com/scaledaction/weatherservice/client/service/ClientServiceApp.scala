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
    val ssc = getActiveOrCreateStreamingContext(cassandraConfig)

    implicit val system = ActorSystem("client-service")

    val precipitation = system.actorOf(Props(new PrecipitationActor(ssc, cassandraConfig)), "precipitation")
    val temperature = system.actorOf(Props(new TemperatureActor(ssc.sparkContext, cassandraConfig)), "temperature")
    val weatherStation = system.actorOf(Props(new WeatherStationActor(ssc.sparkContext, cassandraConfig)), "weather-station")

    val service = system.actorOf(Props(new ClientService(precipitation, temperature, weatherStation)), "client-service")

    startServer(service)

    ssc.start()
    ssc.awaitTermination()
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
