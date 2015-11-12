/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.killrweather

import akka.actor.{ ActorLogging, Actor, ActorRef }
import akka.pattern.pipe
import com.datastax.spark.connector._
import com.scaledaction.core.cassandra.CassandraConfig
import org.apache.spark.SparkContext
import scala.util.Success

/** For a given weather station, calculates annual cumulative precip - or year to date. */
//class PrecipitationActor(ssc: StreamingContext, settings: WeatherSettings)
//class PrecipitationActor(ssc: StreamingContext, cassandraConfig: CassandraConfig,weatherServiceAppConfig: WeatherServiceAppConfig)
class PrecipitationActor(sc: SparkContext, cassandraConfig: CassandraConfig)
    extends AggregationActor with ActorLogging {

    import Weather._
    import WeatherEvent._

    val keyspace = cassandraConfig.keyspace

    //TODO - Add a WeatherServiceAppConfig and replace the hard-coded "dailytable" value
    //    import settings.{CassandraTableDailyPrecip => dailytable}
    //    val CassandraTableDailyPrecip = killrweather.getString("cassandra.table.daily.precipitation")
    //    cassandra {
    //        table.daily.precipitation = "daily_aggregate_precip"
    //    }
    val dailyTable = "daily_aggregate_precip"
    val yearlyTable = "year_cumulative_precip"
    val rawTable = "raw_weather_data"

    def receive: Actor.Receive = {
        case e: GetPrecipitation => cummulative(e, sender)
        case e: GetTopKPrecipitation => topK(e, sender)
    }

    /**
     * Computes and sends the annual aggregation to the `requester` actor.
     * Precipitation values are 1 hour deltas from the previous.
     * TODO: This mechanism is not yet clear. The KW version simply sums the
     * extant values in the daily_aggregate_precip table that are loaded
     * during ingestion. But any involvement of the year_cumulative_precip
     * is not apparent.
     */
    // TODO: Currently dependent on ingestion population of daily_aggregate_precip.
    private def cummulative(e: GetPrecipitation, requester: ActorRef): Unit =
      sc.cassandraTable[Double](keyspace, dailyTable)
      .select("precipitation")
      .where("wsid = ? AND year = ?", e.wsid, e.year)
      .collectAsync()
      .map(toYearlyCumulative(e.wsid, e.year, _)) pipeTo requester
      
    private def toYearlyCumulative(
        wsid: String, year: Int, aggregate: Seq[Double]
    ): WeatherAggregate =
        if (aggregate.nonEmpty) {
            AnnualPrecipitation(wsid, year, sc.parallelize(aggregate).sum)
        } else {
            log.info("PrecipitationActor.toCumulative NoDataAvailable")
            NoDataAvailable(wsid, year, classOf[DailyTemperature])
        }

    /** Returns the k highest temps for any station in the `year`. */
    private def topK(e: GetTopKPrecipitation, requester: ActorRef): Unit = {
        println("---->PrecipitationActor.topK")
        val results = sc.cassandraTable[Double](keyspace, dailyTable) 
            .select("precipitation")
            .where("wsid = ? AND year = ?", e.wsid, e.year)
            .collectAsync() // TODO - Try aggregate instead of collect
            .map(x => x match {
                case Nil => None
                case aggregate => Some(TopKPrecipitation(
                    e.wsid, e.year, sc.parallelize(aggregate).top(e.k).toSeq))
            })
        results pipeTo requester
    }
}
