package com.scaledaction.weatherservice.ingest.api

import spray.routing.Directives
import spray.http.MediaTypes._
import akka.actor.ActorRef
import com.datastax.killrweather.Weather.RawWeatherData

class DataIngestApi(val receiver: ActorRef) extends Directives {

  val route =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            //Thread.sleep(22000)
            //TODO - Implement me
            //receiver ! "RawWeatherData"
            receiver ! RawWeatherData
            <html>
              <body>
                <h1>Weather Service Data Ingest API</h1>
              </body>
            </html>
          }
        }
      }
    }
}
