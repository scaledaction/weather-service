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
 * and TODO: for a given weather station, computes temperature statistics by 
 * month for a given year.
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
    val dailyTable = "daily_aggregate_temperature"
    //  import settings.{CassandraTableRaw => rawtable}
    val rawTable = "raw_weather_data"
    val monthlyTable = "monthly_aggregate_temperature"

    def receive: Actor.Receive = {
        //case e: GetDailyTemperature => daily(e.day, sender)
        case e: GetDailyTemperature => daily(Day(e.wsid, e.year, e.month, e.day), sender)
        case e: GetMonthlyTemperature => highLow(e, sender)
        case e: DailyTemperature => storeDaily(e)
        case e: MonthlyTemperature => storeMonthly(e)
    }

    private def daily(day: Day, requester: ActorRef): Unit =      
        sc.cassandraTable[DailyTemperature](keyspace, dailyTable)
        .where("wsid = ? AND year = ? AND month = ? AND day = ?",
            day.wsid, day.year, day.month, day.day)
        .collectAsync // TODO: write custom sum that checks for no data 
        .map(seqDT => seqDT.headOption match {
            case None => aggregateDaily(day, requester)
            case Some(dailyTemperature) => requester ! dailyTemperature
        })
    
    private def aggregateDaily(day: Day, requester: ActorRef): Unit = {      
        sc.cassandraTable[Double](keyspace, rawTable)
        .select("temperature").where(
            "wsid = ? AND year = ? AND month = ? AND day = ?",
            day.wsid, day.year, day.month, day.day
        )
        .collectAsync() // TODO: write custom sum that checks for no data
        .map(toDaily(_, day)) pipeTo requester
    }

    /**
     * Checks for stored monthly aggregation and if not exists, passes
     * to the aggregateMonth method.
     */
    private def highLow(
        e: GetMonthlyTemperature, requester: ActorRef)
    : Unit = {
        sc.cassandraTable[MonthlyTemperature](keyspace, monthlyTable)
        .where("wsid = ? AND year = ? AND month = ?",
            e.wsid, e.year, e.month)
        .collectAsync // TODO: write custom sum that checks for no data
        .map(seqDT => seqDT.headOption match {
            case None => aggregateMonth(e, requester)
            case Some(monthlyTemperature) => requester ! monthlyTemperature
        })
    }
        
    private def aggregateMonth(
        e: GetMonthlyTemperature, requester: ActorRef
    ): Unit =
        sc.cassandraTable[RawWeatherData](keyspace, rawTable)
        .where("wsid = ? AND year = ? AND month = ?", e.wsid, e.year, e.month)
        .collectAsync() // TODO: write custom sum that checks for no data
        .map(toMonthly(_, e.wsid, e.year, e.month)) pipeTo requester

    private def storeDaily(e: DailyTemperature): Unit =
        sc.parallelize(Seq(e)).saveToCassandra(keyspace, dailyTable)
        
    private def storeMonthly(e: MonthlyTemperature): Unit =
        sc.parallelize(Seq(e)).saveToCassandra(keyspace, monthlyTable)

    private def toDaily(aggregate: Seq[Double], key: Day): WeatherAggregate =
        if (aggregate.nonEmpty) {
            val data = toDailyTemperature(key, StatCounter(aggregate))
            if(aggregate.length >= 24) self ! data
            data
        } else {
            log.info("TemperatureActor.toDaily NoDataAvailable")
            NoDataAvailable(key.wsid, key.year, classOf[DailyTemperature]) 
        }

    private def toMonthly(
        aggregate: Seq[RawWeatherData], wsid: String, year: Int, month: Int)
        : WeatherAggregate = {
        if (aggregate.nonEmpty) {
            val data = MonthlyTemperature(wsid, year, month, 
                aggregate.map(rwd => rwd.temperature).max, 
                aggregate.map(rwd => rwd.temperature).min)
            if(aggregate.length >= monthLength(month)) self ! data
            data
        } 
        else NoDataAvailable(wsid, year, classOf[MonthlyTemperature])
    }
    
    private def toDailyTemperature(key: Day, stats: StatCounter) =
        DailyTemperature(key.wsid, key.year, key.month, key.day,
            high = stats.max, low = stats.min, mean = stats.mean,
            variance = stats.variance, stdev = stats.stdev)

}
