package com.scaledaction.weatherservice.ingest.backend

import com.scaledaction.core.cassandra.HasCassandraConfig
import com.scaledaction.core.kafka.HasKafkaConfig
import com.scaledaction.core.spark.{ SparkUtils, HasSparkConfig }
import com.datastax.killrweather.Weather.RawWeatherData
import com.datastax.spark.connector.streaming._
import kafka.serializer.StringDecoder
import org.apache.spark.Logging
import org.apache.spark.streaming.kafka.KafkaUtils
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.JsonParser

// github/mesosphere/iot-demo/streaming/src/main/scala/com/bythebay/pipeline/spark/streaming/StreamingRatings.scala
// github/scaledaction/killrweather/killrweather-app/src/main/scala/com/datastax/killrweather/KafkaStreamingActor.scala
object DataIngestBackendApp extends App with HasCassandraConfig with HasKafkaConfig with HasSparkConfig with Logging {

  //TODO - Add application config - and replace the following hard-coded Cassandra table name values
  val CassandraTableRaw = "raw_weather_data" //"cassandra.table.raw"
  val CassandraTableDailyPrecip = "daily_aggregate_precip" //cassandra.table.daily.precipitation

  val cassandraConfig = getCassandraConfig

  val kafkaConfig = getKafkaConfig

  val sparkConfig = getSparkConfig

  //TODO - Need to add ApplicationConfig and replace the hard-coded "sparkAppName" value with application.app-name
  val ssc = SparkUtils.getActiveOrCreateStreamingContext(cassandraConfig, sparkConfig.master, "DataIngestBackend")

  //From KW
  //val kafkaStream = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
  //  ssc, kafkaParams, Map(KafkaTopicRaw -> 1), StorageLevel.DISK_ONLY_2)

  implicit val jsonFormats: Formats = DefaultFormats

  //From IoT-Demo
  val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
    ssc, kafkaConfig.kafkaParams, kafkaConfig.topics)
    .map { case (_, v) => JsonParser.parse(v).extract[RawWeatherData] }

  /** Saves the raw data to Cassandra - raw table. */
  kafkaStream.saveToCassandra(cassandraConfig.keyspace, CassandraTableRaw)

  /**
   * For a given weather station, year, month, day, aggregates hourly precipitation values by day.
   * Weather station first gets you the partition key - data locality - which spark gets via the
   * connector, so the data transfer between spark and cassandra is very fast per node.
   *
   * Persists daily aggregate data to Cassandra daily precip table by weather station,
   * automatically sorted by most recent (due to how we set up the Cassandra schema:
   * @see https://github.com/killrweather/killrweather/blob/master/data/create-timeseries.cql.
   *
   * Because the 'oneHourPrecip' column is a Cassandra Counter we do not have to do a spark
   * reduceByKey, which is expensive. We simply let Cassandra do it - not expensive and fast.
   * This is a Cassandra 2.1 counter functionality ;)
   *
   * This new functionality in Cassandra 2.1.1 is going to make time series work even faster:
   * https://issues.apache.org/jira/browse/CASSANDRA-6602
   */
  kafkaStream.map { weather =>
    (weather.wsid, weather.year, weather.month, weather.day, weather.oneHourPrecip)
  }.saveToCassandra(cassandraConfig.keyspace, CassandraTableDailyPrecip)

  kafkaStream.print // for demo purposes only  

  ssc.start()
  ssc.awaitTermination()
}
