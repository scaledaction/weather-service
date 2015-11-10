package com.scaledaction.weatherservice.ingest.api

import org.apache.spark.Logging
import org.apache.spark.streaming.kafka.KafkaUtils
import kafka.serializer.StringDecoder
import com.scaledaction.core.cassandra.HasCassandraConfig
import com.scaledaction.core.spark.SparkUtils
import com.scaledaction.core.kafka.HasKafkaConfig
//import com.datastax.killrweather.WeatherEvent._
//import com.datastax.killrweather.Weather._

//TODO - Implement me
// github/mesosphere/iot-demo/streaming/src/main/scala/com/bythebay/pipeline/spark/streaming/StreamingRatings.scala
// github/scaledaction/killrweather/killrweather-app/src/main/scala/com/datastax/killrweather/KafkaStreamingActor.scala
object DataIngestBackendApp extends App with HasCassandraConfig with HasKafkaConfig with Logging {

  //import settings._
  //import WeatherEvent._
  //import Weather._

  val cassandraConfig = getCassandraConfig

  val kafkaConfig = getKafkaConfig

  //TODO - Need to add SparkConfig and replace the hard-coded "sparkMaster" and "sparkAppName" value
  val ssc = SparkUtils.getActiveOrCreateStreamingContext(cassandraConfig, "local[3]", "DataIngestBackend")

  //From KW
  //  val kafkaStream = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
  //    ssc, kafkaParams, Map(KafkaTopicRaw -> 1), StorageLevel.DISK_ONLY_2)

  //  val kafkaBrokers = sys.env("TWEET_CONSUMER_KAFKA_BROKERS")
  //  val kafkaTopics = sys.env("TWEET_CONSUMER_KAFKA_TOPIC").split(",").toSet
  //  val kafkaParams = Map[String, String]("metadata.broker.list" -> kafkaBrokers)
  //
  //  //From IoT-Demo
  //  val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
  //    ssc, kafkaParams, kafkaTopics)
  //    .map(_._2.split(","))
  //TODO - use the JSON implicit val RawWeatherFormat = jsonFormat14(RawWeatherData) unmarshaller here, 
  // cannot use RawWeatherData(_)  
  //    .map(RawWeatherData(_))
  //
  //  /** Saves the raw data to Cassandra - raw table. */
  //  kafkaStream.saveToCassandra(CassandraKeyspace, CassandraTableRaw)
  //
  //  /**
  //   * For a given weather station, year, month, day, aggregates hourly precipitation values by day.
  //   * Weather station first gets you the partition key - data locality - which spark gets via the
  //   * connector, so the data transfer between spark and cassandra is very fast per node.
  //   *
  //   * Persists daily aggregate data to Cassandra daily precip table by weather station,
  //   * automatically sorted by most recent (due to how we set up the Cassandra schema:
  //   * @see https://github.com/killrweather/killrweather/blob/master/data/create-timeseries.cql.
  //   *
  //   * Because the 'oneHourPrecip' column is a Cassandra Counter we do not have to do a spark
  //   * reduceByKey, which is expensive. We simply let Cassandra do it - not expensive and fast.
  //   * This is a Cassandra 2.1 counter functionality ;)
  //   *
  //   * This new functionality in Cassandra 2.1.1 is going to make time series work even faster:
  //   * https://issues.apache.org/jira/browse/CASSANDRA-6602
  //   */
  //  kafkaStream.map { weather =>
  //    (weather.wsid, weather.year, weather.month, weather.day, weather.oneHourPrecip)
  //  }.saveToCassandra(CassandraKeyspace, CassandraTableDailyPrecip)
  //
  //  kafkaStream.print // for demo purposes only  

  ssc.start()
  ssc.awaitTermination()
}