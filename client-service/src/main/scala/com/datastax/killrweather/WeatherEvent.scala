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

// TODO document the Event API
object WeatherEvent extends DefaultJsonProtocol {
  import Weather._

  /** Base marker trait. */
  @SerialVersionUID(1L)
  sealed trait WeatherEvent extends Serializable

  sealed trait LifeCycleEvent extends WeatherEvent
  case object OutputStreamInitialized extends LifeCycleEvent
  case object NodeInitialized extends LifeCycleEvent
  case object Start extends LifeCycleEvent
  case object DataFeedStarted extends LifeCycleEvent
  case object Shutdown extends LifeCycleEvent
  case object TaskCompleted extends LifeCycleEvent

  sealed trait WeatherRequest extends WeatherEvent
  trait WeatherStationRequest extends WeatherRequest
  case class GetWeatherStation(sid: String) extends WeatherStationRequest
  case class GetCurrentWeather(wsid: String, timestamp: Option[DateTime]= None) extends WeatherStationRequest

  trait PrecipitationRequest extends WeatherRequest
  case class GetPrecipitation(wsid: String, year: Int) extends PrecipitationRequest
  case class GetTopKPrecipitation(wsid: String, year: Int, k: Int) extends PrecipitationRequest
  implicit val GetPrecipitationFormat = jsonFormat2(GetPrecipitation)

  trait TemperatureRequest extends WeatherRequest
  //TODO - get the following message to marshall a Day from the HTTP json
  //case class GetDailyTemperature(day: Day) extends TemperatureRequest
  case class GetDailyTemperature(wsid: String, year: Int, month: Int, day: Int) extends TemperatureRequest  
  case class GetMonthlyHiLowTemperature(wsid: String, year: Int, month: Int) extends TemperatureRequest
  case class GetMonthlyTemperature(wsid: String, year: Int, month: Int) extends TemperatureRequest
  implicit val DayFormat = jsonFormat4(Day)
  //implicit val GetDailyTemperatureFormat = jsonFormat1(GetDailyTemperature)
  implicit val GetDailyTemperatureFormat = jsonFormat4(GetDailyTemperature)

  sealed trait Task extends Serializable
  case object QueryTask extends Task
  case object GracefulShutdown extends LifeCycleEvent

  /**
   * Quick access lookup table for sky_condition. Useful for potential analytics.
   * See http://en.wikipedia.org/wiki/Okta
   */
  case class GetSkyConditionLookup(code: Int) extends WeatherRequest

}
