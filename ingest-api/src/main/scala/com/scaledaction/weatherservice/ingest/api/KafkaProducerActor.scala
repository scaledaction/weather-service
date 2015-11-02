package com.scaledaction.weatherservice.ingest.api

import akka.event.Logging
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord, Callback, RecordMetadata }
import akka.actor._
import com.scaledaction.core.kafka.HasKafkaConfig
import com.datastax.killrweather.Weather.RawWeatherData
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol

class KafkaProducerActor extends Actor with HasKafkaConfig {
  //TODO - set up a logging trait like spark has
  val log = Logging(context.system, this)

  val kafkaConfig = getKafkaConfig
  println(s"kafkaConfig[$kafkaConfig]")

  val producer = new KafkaProducer[String, String](kafkaConfig.toProducerProperties)

  def receive: Receive = {
    case weatherData: RawWeatherData =>
      val record = new ProducerRecord[String, String](
        kafkaConfig.topic, weatherData.toJson.toString)
      println(s"KafkaTopic: ${kafkaConfig.topic}, weatherData: ${weatherData.toJson}")
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
