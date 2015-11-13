/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.scaledaction.core.kafka

import com.scaledaction.core.config.{ AppConfig, HasAppConfig }
import com.typesafe.config.Config
import java.util.Properties
import org.apache.kafka.clients.producer.ProducerConfig
import scala.util.Try

/**
 * Application settings. First attempts to acquire from the deploy environment.
 * If not exists, then from -D java system properties, else a default config.
 *
 * Settings in the environment such as: SPARK_HA_MASTER=local[10] is picked up first.
 *
 * Settings from the command line in -D will override settings in the deploy environment.
 * For example: sbt -Dspark.master="local[12]" run
 *
 * If you have not yet used Typesafe Config before, you can pass in overrides like so:
 *
 * {{{
 *   new Settings(ConfigFactory.parseString("""
 *      spark.master = "some.ip"
 *   """))
 * }}}
 *
 * Any of these can also be overridden by your own application.conf.
 *
 * @param conf Optional config for test
 */
// brokers is a comma-separated list
class KafkaConfig(
  val brokers: String,
  val topic: String,
  val keySerializer: String,
  val valueSerializer: String,
  rootConfig: Config) extends AppConfig(rootConfig: Config) {

  def toProducerProperties: Properties = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer)
    props
  }

  val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)

  val topics = topic.split(",").toSet

  override def toString(): String = s"brokers: ${brokers}, topic: ${topic}, keySerializer: ${keySerializer}, valueSerializer: ${valueSerializer}"
}

trait HasKafkaConfig extends HasAppConfig {

  def getKafkaConfig: KafkaConfig = getKafkaConfig(rootConfig.getConfig("kafka"))

  def getKafkaConfig(rootName: String): KafkaConfig = getKafkaConfig(rootConfig.getConfig(rootName))

  //kafka {
  //  brokers = ["127.0.0.1:9092"]
  //  brokers = [${?KAFKA_BROKERS}]
  //  
  //  topic = "killrweather.raw"
  //  topic = [${?KAFKA_TOPIC}]
  //  
  //  key_serializer = "org.apache.kafka.common.serialization.StringSerializer"
  //  value_serializer = "org.apache.kafka.common.serialization.StringSerializer"
  //  
  //  #ingest-rate = 1s
  //  #group.id = "killrweather.group"
  //  #batch.send.size = 100   
  //}

  private def getKafkaConfig(kafka: Config): KafkaConfig = {

    val brokers = getRequiredValue("KAFKA_BROKERS", (kafka, "brokers"), "127.0.0.1:9092")

    val topic = getRequiredValue("KAFKA_TOPIC", (kafka, "topic"), "killrweather.raw")

    val keySerializer = getRequiredValue((kafka, "key_serializer"), "org.apache.kafka.common.serialization.StringSerializer")

    val valueSerializer = getRequiredValue((kafka, "value_serializer"), "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaConfig(brokers, topic, keySerializer, valueSerializer, kafka)
  }

  //  val KafkaGroupId = kafka.getString("group.id")
  //  val KafkaTopicRaw = kafka.getString("topic.raw")
  //  val KafkaEncoderFqcn = kafka.getString("encoder.fqcn")
  //  val KafkaDecoderFqcn = kafka.getString("decoder.fqcn")
  //  val KafkaPartitioner = kafka.getString("partitioner.fqcn")
  //  val KafkaBatchSendSize = kafka.getInt("batch.send.size")
}
