package com.scaledaction.weatherservice.client.service

import spray.routing.Directives
import spray.http.MediaTypes._
import akka.actor.ActorRef
//import com.datastax.killrweather.Weather.RawWeatherData
import spray.httpx.SprayJsonSupport._

//class ClientService(val receiver: ActorRef) extends Directives {
class ClientService() extends Directives {

  val route =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            //Thread.sleep(22000)
            //TODO  Implement me
            //receiver ! "RawWeatherData"
            <html>
              <body>
                <h1>Weather Service Client API</h1>
              </body>
            </html>
          }
        }
      }
    }
}
