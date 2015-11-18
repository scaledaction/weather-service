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
        case e: GetPrecipitation => yearlyCummulative(e, sender)
        case e: GetTopKPrecipitation => yearlyTopK(e, sender)
        case e: AnnualPrecipitation => storeAnnualPrecip(e)
    }

    private def yearlyCummulative(
        e: GetPrecipitation, requester: ActorRef): Unit = {
        sc.cassandraTable[AnnualPrecipitation](keyspace, yearlyTable)
        .where("wsid = ? AND year = ?", e.wsid, e.year)
        .collectAsync // TODO: write custom sum that checks for no data
        .map(seqAP => seqAP.headOption match {
            case None => aggregateYearly(e, requester)
            case Some(yearlyPrecip) => requester ! yearlyPrecip
        })
    }

    // TODO: Consider how to use //.sum() TODO See DoubleRDDFunctions //import org.apache.spark.SparkContext._
    // TODO: Dependent on population of daily_aggregate_precip via ingest-api.
    private def aggregateYearly(
        e: GetPrecipitation, requester: ActorRef
    ): Unit = {
        sc.cassandraTable[Double](keyspace, dailyTable) // TODO: Needs to read from raw data since this is a derivation it cannot be assumed extant without a check.
        .select("precipitation")
        .where("wsid = ? AND year = ?", e.wsid, e.year)
        .collectAsync() // TODO: use Spark aggregate function
        .map(yearlyCummulative(e.wsid, e.year, _)) pipeTo requester
    }
      
    private def yearlyCummulative(
        wsid: String, year: Int, aggregate: Seq[Double]
    ): WeatherAggregate =
        if (aggregate.nonEmpty) {
            val data = AnnualPrecipitation(
                wsid, year, sc.parallelize(aggregate).sum / 10) /* TODO: daily_aggregate_precip precipitation is a Cassandra counter which only holds Int, so we multiply and divide by 10. Values resolve to 1 decimal place. We are note attemptingto batch or store in year_cumulative_precip table as the lambda architecture pattern is not yet clear. */
            if(timestamp.getYear > year) self ! data   
            data
        }
        else NoDataAvailable(wsid, year, classOf[AnnualPrecipitation])
        /* TODO RW: Implement calculation attempt of AP from raw data?
         * First build daily_aggregate_precip for year if available
         * (perhaps only partially?)? Or just build only the
         * year_aggregate_precip? What are the possible error scenarios?
         * To what degree should error scenarios be detected and 
         * accounted for (i.e. recovered)?
         * I think it is too much responsibility for each of these query
         * methods to manage in terms of rebuilding aggregation views.
         * There should be batch processes for such recovery or filling
         * in missing data.*/

    /** Returns the k highest temps for any station in the `year`. */
    private def yearlyTopK(
        e: GetTopKPrecipitation, requester: ActorRef)
    : Unit = {
        val results = sc.cassandraTable[Double](keyspace, dailyTable) 
            .select("precipitation")
            .where("wsid = ? AND year = ?", e.wsid, e.year)
            .collectAsync() // TODO - Try aggregate instead of collect
            .map(seqPrecip => seqPrecip match {
                case Nil => 
                    NoDataAvailable(e.wsid, e.year, classOf[TopKPrecipitation])
                case seqPrecip => 
                    TopKPrecipitation(
                        e.wsid, e.year, 
                        sc.parallelize(seqPrecip).top(e.k).toSeq.map(p => p/10)
                        // precip stored as Int*10 on ingest, Int/10 on query.
                    )
            })
        results pipeTo requester
    }
    
    private def storeAnnualPrecip(e: AnnualPrecipitation): Unit =
        sc.parallelize(Seq(e)).saveToCassandra(keyspace, yearlyTable)
}
