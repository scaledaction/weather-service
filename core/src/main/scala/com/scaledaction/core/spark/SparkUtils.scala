package com.scaledaction.core.spark

import com.scaledaction.core.cassandra.CassandraConfig
import org.apache.spark.{ SparkContext, SparkConf, Logging }
import org.apache.spark.streaming.{ StreamingContext, Seconds }

object SparkUtils extends Logging {

  //TODO - Need to replace the "sparkMaster" and "sparkAppName" arguments with a SparkConfig argument 
  def getActiveOrCreateSparkContext(cassandraConfig: CassandraConfig, sparkMaster: String, sparkAppName: String): SparkContext = {
    val conf = new SparkConf()
      .set("spark.cassandra.connection.host", cassandraConfig.seednodes)
      .setMaster(sparkMaster)
      .setAppName(sparkAppName)

    SparkContext.getOrCreate(conf)
  }

  //TODO - Need to replace the "sparkMaster" and "sparkAppName" arguments with a SparkConfig argument 
  def getActiveOrCreateStreamingContext(cassandraConfig: CassandraConfig, sparkMaster: String, sparkAppName: String): StreamingContext = {
    val conf = new SparkConf()
      .set("spark.cassandra.connection.host", cassandraConfig.seednodes)
      .setMaster(sparkMaster)
      .setAppName(sparkAppName)

    val sc = SparkContext.getOrCreate(conf)

    def createStreamingContext(): StreamingContext = {
      @transient val newSsc = new StreamingContext(sc, Seconds(2))
      logInfo(s"Creating new StreamingContext $newSsc")

      newSsc
    }
    StreamingContext.getActiveOrCreate(createStreamingContext)
  }
}