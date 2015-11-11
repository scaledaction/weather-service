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
    val dailytable = "daily_aggregate_precip"

    def receive: Actor.Receive = {
        case GetPrecipitation(wsid, year) => cumulative(wsid, year, sender)
        case GetTopKPrecipitation(wsid, year, k) => topK(wsid, year, k, sender)
    }

    /**
     * Computes and sends the annual aggregation to the `requester` actor.
     * Precipitation values are 1 hour deltas from the previous.
     */
    def cumulative(wsid: String, year: Int, requester: ActorRef): Unit = {
        println("---->PrecipitationActor.cumulative")
        val results = sc.cassandraTable[Double](keyspace, dailytable)
            .select("precipitation")
            .where("wsid = ? AND year = ?", wsid, year)
            .collectAsync()
            .map(seq => seq match {
                case Nil => 
                    println("---->PrecipitationActor.cumulative case Nil => None")
                    None
                case aggregate => Some(AnnualPrecipitation(wsid, year, aggregate.sum))
            })

        results pipeTo requester
    }

    /** Returns the k highest temps for any station in the `year`. */
    def topK(wsid: String, year: Int, k: Int, requester: ActorRef): Unit = {
        println("---->PrecipitationActor.topK")
        val results = sc.cassandraTable[Double](keyspace, dailytable)
            .select("precipitation")
            .where("wsid = ? AND year = ?", wsid, year)
            .collectAsync() // TODO - Try aggregate instead of collect
            .map(x => x match {
                case Nil => None
                case aggregate => Some(TopKPrecipitation(
                    wsid, year, sc.parallelize(aggregate).top(k).toSeq))
            })
        results pipeTo requester
    }
}
