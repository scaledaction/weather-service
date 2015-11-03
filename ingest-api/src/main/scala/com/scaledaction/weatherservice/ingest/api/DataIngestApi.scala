package com.scaledaction.weatherservice.ingest.api

import spray.routing.Directives
import spray.http.MediaTypes._
import akka.actor.ActorRef
import com.datastax.killrweather.Weather.RawWeatherData
import spray.httpx.SprayJsonSupport._

//http POST http://127.0.0.1:8081/weather/data/json wsid="724940:23234" year:=2008 month:=1 day:=1 hour:=0 temperature:=11.7 dewpoint:=-0.6 pressure:=1023.8 windDirection:=50 windSpeed:=7.2 skyCondition:=2 skyConditionText="" oneHourPrecip:=0.0 sixHourPrecip:=0.0

class DataIngestApi(val receiver: ActorRef) extends Directives {

    val route = post {
        path("weather"/"data"/"json") {
            handleWith { rawRecord: RawWeatherData =>
                receiver ! rawRecord
                rawRecord
            }
        }
    }
}
