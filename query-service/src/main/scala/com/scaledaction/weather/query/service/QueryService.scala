package com.scaledaction.weather.query.service

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

class QueryService(sc: SparkContext) extends HttpServiceActor with HasCassandraConfig {

    implicit val timeout = stringToDuration("20 s")
    implicit def executionContext = context.dispatcher
    
    val cassandraConfig = getCassandraConfig
    
    val precipitation = context.actorOf(Props(new PrecipitationActor(sc, cassandraConfig)), "precipitation")
    val temperature = context.actorOf(Props(new TemperatureActor(sc, cassandraConfig)), "temperature")
    val weatherStation = context.actorOf(Props(new WeatherStationActor(sc, cassandraConfig)), "weather-station")

    def receive = runRoute(route)

    def route = precipitationRoute ~ temperatureRoute ~ weatherStationRoute

    // Note: Entities must be ordered per decending number of fields.
    
    val precipitationRoute = pathPrefix("weather" / "precipitation") {
        get {
            entity(as[GetTopKPrecipitation]) { e =>
                onSuccess(getTopKPrecipitation(e)) {
                    aggregate => aggregate match {
                        case nda: NoDataAvailable => complete(NotFound)
                        case ap: TopKPrecipitation => complete(OK, ap)
                    }
                }
            } ~
            entity(as[GetPrecipitation]) { e =>
                onSuccess(getPrecipitation(e)) {
                    aggregate => aggregate match {
                        case nda: NoDataAvailable => complete(NotFound)
                        case ap: AnnualPrecipitation => complete(OK, ap)
                    }
                }
            }
        }
    }
    
    val temperatureRoute = pathPrefix("weather" / "temperature") {
        get {
            entity(as[GetDailyTemperature]) { e =>
                onSuccess(getDailyTemperature(e)) {
                    aggregate => aggregate match {
                        case nda: NoDataAvailable => complete(NotFound)
                        case dt: DailyTemperature => complete(OK, dt)
                    }
                }
                //onFailure(magnet) TODO ?
            } ~
            entity(as[GetMonthlyTemperature]) { e =>
                onSuccess(getMonthlyTemperature(e)) {
                    aggregate => aggregate match {
                        case nda: NoDataAvailable => complete(NotFound)
                        case mt: MonthlyTemperature => complete(OK, mt)
                    }
                }
            }
        }
    }
    
    val weatherStationRoute = 
        get {
            pathPrefix("weather" / "station") {
                entity(as[GetWeatherStation]) { e =>
                    onSuccess(getWeatherStation(e)) {
                        case nda: NoDataAvailable => complete(NotFound)
                        case ws: WeatherStation => complete(OK, ws)
                    }
                }
            } ~
            pathPrefix("weather" / "current") {
                entity(as[GetCurrentWeather]) { e =>
                    onSuccess(getCurrentWeather(e)) {
                        case nda: NoDataAvailable => complete(NotFound)
                        case rwd: RawWeatherData => complete(OK, rwd) 
                    }
                }
            }
        }
                
    def getWeatherStation(e: GetWeatherStation) =
        weatherStation.ask(e).mapTo[WeatherModel]
    
    def getCurrentWeather(e: GetCurrentWeather) =
        weatherStation.ask(e).mapTo[WeatherModel]
    
    def getPrecipitation(e: GetPrecipitation) =
        precipitation.ask(e).mapTo[WeatherAggregate]
    
    def getTopKPrecipitation(e: GetTopKPrecipitation) =
        precipitation.ask(e).mapTo[WeatherAggregate]  
    
    def getDailyTemperature(e: GetDailyTemperature) =
        temperature.ask(e).mapTo[WeatherAggregate]
    
    def getMonthlyTemperature(e: GetMonthlyTemperature) =
        temperature.ask(e).mapTo[WeatherAggregate]
}
