package com.scaledaction.weatherservice.ingest.api

import java.util.Properties
import akka.event.Logging
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerConfig }
import akka.actor._
import org.apache.kafka.clients.producer.{ ProducerRecord, Callback, RecordMetadata }
import com.scaledaction.core.config.KafkaConfig
import com.datastax.killrweather.Weather.RawWeatherData
//import kafka.server.KafkaConfig
//import domain.Tweet
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol

class KafkaProducerActor extends Actor with KafkaConfig {
  //TODO - set up a logging trait like spark has
  val log = Logging(context.system, this)

  //  val kafkaBrokers = sys.env("TWEET_PRODUCER_KAFKA_BROKERS")
  //  val kafkaTopic = sys.env("TWEET_PRODUCER_KAFKA_TOPIC")
  //
  //  val props = new Properties()
  //  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
  //  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  //  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  //
  
  // TODO: configuration instead of hardcoded...
  val kafkaTopic = "killrweather.raw"
  val kafkaBroker = "127.0.0.1:9092"

  val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafka.getString("key_serializer"))
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafka.getString("value_serializer"))

  val producer = new KafkaProducer[String, String](props)
  println(s"producer: $producer")

  def receive: Receive = {
    case weatherData: RawWeatherData => 
      val record = new ProducerRecord[String,String](
          kafkaTopic, weatherData.toJson.toString())
      println(s"KafkaTopic: ${KafkaTopic}, weatherData: ${weatherData.toJson}")
          producer.send(record, new Callback {
              override def onCompletion(result: RecordMetadata, exception: Exception) {
                  println(s"result: $result")
                  if (exception != null) {
                      log.warning("Failed to send record", exception)
                  }
              }
          })
  }
}
