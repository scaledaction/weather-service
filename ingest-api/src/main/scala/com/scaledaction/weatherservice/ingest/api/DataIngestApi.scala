package com.scaledaction.weatherservice.ingest.api

import spray.routing.Directives
import spray.http.MediaTypes._
import akka.actor.ActorRef
import com.datastax.killrweather.Weather.RawWeatherData
import spray.httpx.SprayJsonSupport._

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
