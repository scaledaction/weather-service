package com.scaledaction.weatherservice.client.service

import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.killrweather.WeatherEvent._
import com.datastax.killrweather.Weather.{AnnualPrecipitation, TopKPrecipitation}
import scala.concurrent.duration._
import org.apache.spark.SparkContext
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpServiceActor
import com.datastax.killrweather.Weather._
import com.datastax.killrweather.{ PrecipitationActor, TemperatureActor, WeatherStationActor }
import com.scaledaction.core.cassandra.{ CassandraConfig, HasCassandraConfig }

//http GET http://127.0.0.1:8080/weather/precipitation wsid="724940:23234" year:=2008

class ClientService(sc: SparkContext) extends HttpServiceActor with HasCassandraConfig {

  implicit val timeout = stringToDuration("20 s")
  implicit def executionContext = context.dispatcher
  
  val cassandraConfig = getCassandraConfig
  
  // Children supervised by ClientService
  val precipitation = context.actorOf(Props(new PrecipitationActor(sc, cassandraConfig)), "precipitation")
  val temperature = context.actorOf(Props(new TemperatureActor(sc, cassandraConfig)), "temperature")
  val weatherStation = context.actorOf(Props(new WeatherStationActor(sc, cassandraConfig)), "weather-station")

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
      } ~
      entity(as[GetTopKPrecipitation]) { precip =>
        onSuccess(getTopKPrecipitation(precip)) {
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
    precipitation.ask(precip).mapTo[Option[AnnualPrecipitation]]
  
  def getTopKPrecipitation(precip: GetTopKPrecipitation) =
    precipitation.ask(precip).mapTo[Option[TopKPrecipitation]] 

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
    temperature.ask(temp).mapTo[WeatherAggregate]
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
