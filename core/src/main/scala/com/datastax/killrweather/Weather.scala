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

import org.joda.time.DateTime
import spray.json.DefaultJsonProtocol

object Weather extends DefaultJsonProtocol {

  /** Base marker trait. */
  @SerialVersionUID(1L)
  sealed trait WeatherModel extends Serializable

  /**
   * @param id Composite of Air Force Datsav3 station number and NCDC WBAN number
   * @param name Name of reporting station
   * @param countryCode 2 letter ISO Country ID // TODO restrict
   * @param callSign International station call sign
   * @param lat Latitude in decimal degrees
   * @param long Longitude in decimal degrees
   * @param elevation Elevation in meters
   */
  case class WeatherStation(
    id: String,
    name: String,
    countryCode: String,
    callSign: String,
    lat: Double,
    long: Double,
    elevation: Double) extends WeatherModel
    
  implicit val WeatherStationFormat = jsonFormat7(WeatherStation)

  /**
   * @param wsid Composite of Air Force Datsav3 station number and NCDC WBAN number
   * @param year Year collected
   * @param month Month collected
   * @param day Day collected
   * @param hour Hour collected
   * @param temperature Air temperature (degrees Celsius)
   * @param dewpoint Dew point temperature (degrees Celsius)
   * @param pressure Sea level pressure (hectopascals)
   * @param windDirection Wind direction in degrees. 0-359
   * @param windSpeed Wind speed (meters per second)
   * @param skyCondition Total cloud cover (coded, see format documentation)
   * @param skyConditionText Non-coded sky conditions
   * @param oneHourPrecip One-hour accumulated liquid precipitation (millimeters)
   * @param sixHourPrecip Six-hour accumulated liquid precipitation (millimeters)
   */
  case class RawWeatherData(
    wsid: String,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    temperature: Double,
    dewpoint: Double,
    pressure: Double,
    windDirection: Int,
    windSpeed: Double,
    skyCondition: Int,
    skyConditionText: String,
    oneHourPrecip: Double,
    sixHourPrecip: Double
  ) extends WeatherModel

  // Using the default json marshalling.
  implicit val RawWeatherFormat = jsonFormat14(RawWeatherData)

  trait WeatherAggregate extends WeatherModel with Serializable {
    def wsid: String
    def year: Int
  }

  case class Day(wsid: String, year: Int, month: Int, day: Int) extends WeatherAggregate

    /*object Day {
      def apply(d: RawWeatherData): Day =
        Day(d.wsid, d.year, d.month, d.day)
  
      def apply(wsid: String, utcTimestamp: DateTime): Day =
        Day(wsid, utcTimestamp.getYear, utcTimestamp.getMonthOfYear, utcTimestamp.getDayOfMonth)
  
      /** Tech debt */
      def apply(line: String): Day = {
        val array = line.split(",")
        Day(wsid = array(0), year = array(1).toInt, month = array(2).toInt, day = array(3).toInt)
      }
    }*/

  case class NoDataAvailable(wsid: String, year: Int, query: Class[_ <: WeatherAggregate]) extends WeatherAggregate

  /* Precipitation */
  trait Precipitation extends WeatherAggregate

  case class DailyPrecipitation(wsid: String,
                                year: Int,
                                month: Int,
                                day: Int,
                                precipitation: Double) extends Precipitation

  case class AnnualPrecipitation(wsid: String,
                                 year: Int,
                                 precipitation: Double) extends Precipitation
                                 
  implicit val AnnualPrecipitationFormat = jsonFormat3(AnnualPrecipitation)
                                 
//  object AnnualPrecipitation {
//    def apply(aggregate: Seq[Double], wsid: String, year: Int): AnnualPrecipitation =
//      AnnualPrecipitation(wsid, year, aggregate.sum)
//  }
  case class TopKPrecipitation(wsid: String,
                               year: Int,
                               top: Seq[Double]) extends WeatherAggregate
                               
  implicit val TopKPrecipitationFormat = jsonFormat3(TopKPrecipitation)

  /* Temperature */
  trait Temperature extends WeatherAggregate

  case class DailyTemperature(wsid: String,
                              year: Int,
                              month: Int,
                              day: Int,
                              high: Double,
                              low: Double,
                              mean: Double,
                              variance: Double,
                              stdev: Double) extends Temperature
  implicit val DailyTemperatureFormat = jsonFormat9(DailyTemperature)

  case class MonthlyTemperature(wsid: String,
                                year: Int,
                                month: Int,
                                high: Double,
                                low: Double) extends Temperature
  implicit val MonthlyTemperatureFormat = jsonFormat5(MonthlyTemperature)
}
