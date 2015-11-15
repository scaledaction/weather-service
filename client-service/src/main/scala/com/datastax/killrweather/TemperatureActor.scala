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
package com.datastax.killrweather

import akka.actor.{ ActorLogging, Actor, ActorRef }
import akka.pattern.pipe
import com.datastax.spark.connector._
import com.scaledaction.core.cassandra.CassandraConfig
import org.apache.spark.SparkContext
import org.apache.spark.util.StatCounter

/**
 * The TemperatureActor reads the daily temperature rollup data from Cassandra,
 * and for a given weather station, computes temperature statistics by month for a given year.
 */
//class TemperatureActor(sc: SparkContext, settings: WeatherSettings)
class TemperatureActor(sc: SparkContext, cassandraConfig: CassandraConfig)
    extends AggregationActor with ActorLogging {

    import WeatherEvent._
    import Weather._

    val keyspace = cassandraConfig.keyspace

    //TODO - Add a WeatherServiceAppConfig and replace the hard-coded "dailytable" and "rawtable" values
    //  cassandra {
    //    table.raw = "raw_weather_data"
    //    table.daily.temperature = "daily_aggregate_temperature"
    //  }
    //  import settings.{CassandraTableDailyTemp => dailytable}
    val dailytable = "daily_aggregate_temperature"
    //  import settings.{CassandraTableRaw => rawtable}
    val rawtable = "raw_weather_data"

    def receive: Actor.Receive = {
        //case e: GetDailyTemperature => daily(e.day, sender)
        case e: GetDailyTemperature => daily(Day(e.wsid, e.year, e.month, e.day), sender)
        case e: DailyTemperature => store(e)
        case e: GetMonthlyHiLowTemperature => highLow(e, sender)
    }

    /**
     * Computes and sends the daily aggregation to the `requester` actor.
     * We aggregate this data on-demand versus in the stream.
     *
     * For the given day of the year, aggregates 0 - 23 temp values to statistics:
     * high, low, mean, std, etc., and persists to Cassandra daily temperature table
     * by weather station, automatically sorted by most recent - due to our cassandra schema -
     * you don't need to do a sort in spark.
     *
     * Because the gov. data is not by interval (window/slide) but by specific date/time
     * we look for historic data for hours 0-23 that may or may not already exist yet
     * and create stats on does exist at the time of request.
     */
    private def daily(day: Day, requester: ActorRef): Unit =      
        sc.cassandraTable[DailyTemperature](keyspace, dailytable)
        .where("wsid = ? AND year = ? AND month = ? AND day = ?",
            day.wsid, day.year, day.month, day.day)
        .collectAsync
        .map(seqDT => seqDT.headOption match {
            case None => aggregateDaily(day, requester)
            case Some(dailyTemperature) => requester ! dailyTemperature
        })
    
    private def aggregateDaily(day: Day, requester: ActorRef): Unit = {      
        val NIX_diagnostic = sc.cassandraTable[Double](keyspace, rawtable)
        .select("temperature").where(
            "wsid = ? AND year = ? AND month = ? AND day = ?",
            day.wsid, day.year, day.month, day.day
        )
        .collectAsync()
        .map(toDaily(_, day)) pipeTo requester
    }

    /**
     * Computes and sends the monthly aggregation to the `requester` actor.
     */
    private def highLow(e: GetMonthlyHiLowTemperature, requester: ActorRef): Unit =
        sc.cassandraTable[DailyTemperature](keyspace, dailytable)
            .where("wsid = ? AND year = ? AND month = ?", e.wsid, e.year, e.month)
            .collectAsync()
            .map(toMonthly(_, e.wsid, e.year, e.month)) pipeTo requester

    /**
     * Stores the daily temperature aggregates asynchronously which are triggered
     * by on-demand requests during the `forDay` function's `self ! data`
     * to the daily temperature aggregation table.
     */
    private def store(e: DailyTemperature): Unit =
        sc.parallelize(Seq(e)).saveToCassandra(keyspace, dailytable)

    /**
     * Would only be handling 0-23 small items.
     * We do 'self ! data' to async persist the aggregated data
     * but still return it immediately to the requester, vs make client wait.
     *
     * @return If no hourly data available, returns [[NoDataAvailable]]
     *         else [[DailyTemperature]] with mean, variance,stdev,hi,low stats.
     */
    private def toDaily(aggregate: Seq[Double], key: Day): WeatherAggregate =
        if (aggregate.nonEmpty) {
            val data = toDailyTemperature(key, StatCounter(aggregate))
            if(aggregate.length >= 24) self ! data
            data
        } else {
            log.info("TemperatureActor.toDaily NoDataAvailable")
            NoDataAvailable(key.wsid, key.year, classOf[DailyTemperature]) 
        }

    private def toMonthly(aggregate: Seq[DailyTemperature], wsid: String, year: Int, month: Int): WeatherAggregate =
        if (aggregate.nonEmpty)
            MonthlyTemperature(wsid, year, month, aggregate.map(_.high).max, aggregate.map(_.low).min)
        else
            NoDataAvailable(wsid, year, classOf[MonthlyTemperature]) // not wanting to return an option to requester

    private def toDailyTemperature(key: Day, stats: StatCounter): DailyTemperature =
        DailyTemperature(key.wsid, key.year, key.month, key.day,
            high = stats.max, low = stats.min, mean = stats.mean,
            variance = stats.variance, stdev = stats.stdev)
}
