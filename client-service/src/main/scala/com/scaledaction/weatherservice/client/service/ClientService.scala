package com.scaledaction.weatherservice.client.service

import spray.routing.Directives
import spray.http.MediaTypes._
import akka.actor.ActorRef
import spray.httpx.SprayJsonSupport._
import com.datastax.killrweather.WeatherEvent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing._
import com.datastax.killrweather.Weather.AnnualPrecipitation

//class RestApi(timeout: Timeout) extends HttpServiceActor
//    with RestRoutes {
//  implicit val requestTimeout = timeout
//
//  def receive = runRoute(routes)
//
//  implicit def executionContext = context.dispatcher
//
//  def createBoxOffice = context.actorOf(BoxOffice.props, BoxOffice.name)
//}

//http GET http://127.0.0.1:8080/weather/precipitation wsid="724940:23234" year:=2008

//class ClientService(val receiver: ActorRef, timeout: Timeout,  context: ExecutionContext) extends Directives {
class ClientService(val receiver: ActorRef) extends HttpServiceActor {
  //class ClientService() extends Directives {
  //implicit val requestTimeout = timeout
  implicit val timeout = stringToDuration("20 s")
  implicit def executionContext = context.dispatcher

  def receive = runRoute(route)
  //  val route =
  //    path("") {
  //      get {
  //        respondWithMediaType(`text/html`) {
  //          complete {
  //            //Thread.sleep(22000)
  //            //TODO  Implement me
  //            //receiver ! "RawWeatherData"
  //            <html>
  //              <body>
  //                <h1>Weather Service Client API</h1>
  //              </body>
  //            </html>
  //          }
  //        }
  //      }
  //    }

  val route =
    pathPrefix("weather" / Segment) { event =>
      //pathEndOrSingleSlash {
        get {
          // POST /events/:event
          entity(as[GetPrecipitation]) { precip =>
            onSuccess(getEvent(precip)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
//              _.fold(complete(NotFound))(e => complete(OK))
            }
          }
        //}
        //        ~
        //          get {
        //            // GET /events/:event
        //            onSuccess(getEvent(event)) {
        //              _.fold(complete(NotFound))(e => complete(OK, e))
        //            }
        //          } ~
        //          delete {
        //            // DELETE /events/:event
        //            onSuccess(cancelEvent(event)) {
        //              _.fold(complete(NotFound))(e => complete(OK, e))
        //            }
        //          }
      }
    }

  def stringToDuration(t: String): Timeout = {
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }

  def getEvent(precip: GetPrecipitation) =
    receiver.ask(precip)
      .mapTo[Option[AnnualPrecipitation]]
}
//        } ~
//        get {
//          // GET /events/:event
//          onSuccess(getEvent(event)) {
//            _.fold(complete(NotFound))(e => complete(OK, e))
//          }
//        } ~
//        delete {
//          // DELETE /events/:event
//          onSuccess(cancelEvent(event)) {
//            _.fold(complete(NotFound))(e => complete(OK, e))
//          }
//        }
//      }
//    }

// Client Service REST API:
//
//github/killrweather/killrweather/data/load/ny-sf-2008.csv.gz
//725030:14732,2008,01,01,00,5.0,-3.9,1020.4,270,4.6,2,0.0,0.0
//725030:14732,2008,01,01,01,5.0,-3.3,1020.6,290,4.1,2,0.0,0.0
//725030:14732,2008,01,01,02,5.0,-3.3,1020.0,310,3.1,2,0.0,0.0
//725030:14732,2008,01,01,03,4.4,-2.8,1020.1,300,1.5,2,0.0,0.0
//725030:14732,2008,01,01,04,3.3,-4.4,1020.5,240,2.6,0,0.0,0.0
//725030:14732,2008,01,01,05,0.0,999.9,0.0,0,0.0,0,0.0,0.0
//725030:14732,2008,01,01,06,3.3,-2.8,1020.5,210,2.1,0,0.0,0.0
//725030:14732,2008,01,01,07,1.7,-2.8,1019.6,120,3.1,0,0.0,0.0
//725030:14732,2008,01,01,08,2.8,-2.2,1019.7,120,2.6,0,0.0,0.0
//725030:14732,2008,01,01,09,2.2,-2.8,1019.3,100,1.5,0,0.0,0.0
//
//PrecipitationActor
//  def receive : Actor.Receive = {
//    case GetPrecipitation(wsid, year)        => cumulative(wsid, year, sender)
//    case GetTopKPrecipitation(wsid, year, k) => topK(wsid, year, k, sender)
//  }
//
//TemperatureActor
//  def receive: Actor.Receive = {
//    case e: GetDailyTemperature        => daily(e.day, sender)
// This doesn't come directly from HTTP Request:    case e: DailyTemperature           => store(e)
//    case e: GetMonthlyHiLowTemperature => highLow(e, sender)
//  }
//
//WeatherStationActor
//  def receive : Actor.Receive = {
//    case GetCurrentWeather(wsid, dt) => current(wsid, dt, sender)
//    case GetWeatherStation(wsid)     => weatherStation(wsid, sender)
//  }
//
//  def queries(): Unit = {
//
//    val previous = (day: Day) => {
//      val key = day.wsid.split(":")(0)
//      queried.exists(_.month > day.month)
//      // run more queries than queried.exists(_.wsid.startsWith(key)) until more wsid data
//    }
//
//    val toSample = (source: Sources.FileSource) => source.days.filterNot(previous).headOption
//
//    initialData.flatMap(toSample(_)).headOption map { sample =>
//      log.debug("Requesting the current weather for weather station {}", sample.wsid)
//      // because we load from historic file data vs stream in the cloud for this sample app ;)
//      val timestamp = new DateTime(DateTimeZone.UTC).withYear(sample.year)
//        .withMonthOfYear(sample.month).withDayOfMonth(sample.day)
//      guardian ! GetCurrentWeather(sample.wsid, Some(timestamp))
//
//      log.debug("Requesting annual precipitation for weather station {} in year {}", sample.wsid, sample.year)
//      guardian ! GetPrecipitation(sample.wsid, sample.year)
//
//      log.debug("Requesting top-k Precipitation for weather station {}", sample.wsid)
//      guardian ! GetTopKPrecipitation(sample.wsid, sample.year, k = 10)
//
//      log.debug("Requesting the daily temperature aggregate for weather station {}", sample.wsid)
//      guardian ! GetDailyTemperature(sample)
//
//      log.debug("Requesting the high-low temperature aggregate for weather station {}",sample.wsid)
//      guardian ! GetMonthlyHiLowTemperature(sample.wsid, sample.year, sample.month)
//
//      log.debug("Requesting weather station {}", sample.wsid)
//      guardian ! GetWeatherStation(sample.wsid)
//
//      queried += sample
//    }
//  }
