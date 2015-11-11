package com.scaledaction.weatherservice.ingest.backend

import com.datastax.killrweather.Weather.RawWeatherData

//TODO - temp - use the JSON implicit val RawWeatherFormat = jsonFormat14(RawWeatherData) unmarshaller instead, 
//cannot use RawWeatherData(_) 
object RawWeatherDataFactory {
  /** Tech debt - don't do it this way ;) */
  def apply(array: Array[String]): RawWeatherData = {
    RawWeatherData(
      wsid = array(0),
      year = array(1).toInt,
      month = array(2).toInt,
      day = array(3).toInt,
      hour = array(4).toInt,
      temperature = array(5).toDouble,
      dewpoint = array(6).toDouble,
      pressure = array(7).toDouble,
      windDirection = array(8).toInt,
      windSpeed = array(9).toDouble,
      skyCondition = array(10).toInt,
      skyConditionText = array(11),
      oneHourPrecip = array(11).toDouble,
      sixHourPrecip = Option(array(12).toDouble).getOrElse(0))
  }
}