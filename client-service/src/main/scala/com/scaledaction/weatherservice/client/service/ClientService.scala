package com.scaledaction.weatherservice.client.service

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.killrweather.WeatherEvent._
import com.datastax.killrweather.Weather.AnnualPrecipitation
import scala.concurrent.duration._
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpServiceActor
import com.datastax.killrweather.Weather.WeatherAggregate
import com.datastax.killrweather.Weather.NoDataAvailable
import com.datastax.killrweather.Weather.DailyTemperature

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
class ClientService(precipitation: ActorRef, temperature: ActorRef, weatherStation: ActorRef) extends HttpServiceActor {

  implicit val timeout = stringToDuration("20 s")
  implicit def executionContext = context.dispatcher

  def receive = runRoute(route)

  //TODO - Route me
  //def route = precipitationRoute ~ temperatureRoute ~ weatherStationRoute
  def route = precipitationRoute ~ temperatureRoute

  val precipitationRoute = pathPrefix("weather" / "precipitation") {
    get {
      entity(as[GetPrecipitation]) { precip =>
        onSuccess(getPrecipitation(precip)) {
          _.fold(complete(NotFound))(e => complete(OK, e))
        }
      }
    }
  }
  //http://spray.io/documentation/1.2.3/spray-routing/key-concepts/rejections/#rejectionhandler
  //https://groups.google.com/forum/#!topic/spray-user/84mcHgOH4C4
  //https://github.com/spray/spray/wiki/Custom-Error-Responses
  //http://tysonjh.com/blog/2014/05/05/spray-custom-404/  
  def getPrecipitation(precip: GetPrecipitation) =
    precipitation.ask(precip)
      .mapTo[Option[AnnualPrecipitation]]

  val temperatureRoute = pathPrefix("weather" / "dailytemperature") {
    get {
      entity(as[GetDailyTemperature]) { temperature =>
        onSuccess(getDailyTemperature(temperature)) {
          res =>
            res match {
              case nda: NoDataAvailable => complete(NotFound)
              case t: DailyTemperature => complete(OK, t)
            }
        }
      }
    }
  }
  //http://spray.io/documentation/1.2.3/spray-routing/key-concepts/rejections/#rejectionhandler
  //https://groups.google.com/forum/#!topic/spray-user/84mcHgOH4C4
  //https://github.com/spray/spray/wiki/Custom-Error-Responses
  //http://tysonjh.com/blog/2014/05/05/spray-custom-404/  
  def getDailyTemperature(temp: GetDailyTemperature) =
    temperature.ask(temp)
      .mapTo[WeatherAggregate]

  def stringToDuration(t: String): Timeout = {
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
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
