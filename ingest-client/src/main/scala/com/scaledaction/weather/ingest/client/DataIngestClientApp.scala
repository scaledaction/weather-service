package com.scaledaction.weather.ingest.client

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import java.io.{BufferedInputStream, FileInputStream, File => JFile}
import spray.http._
import spray.can.Http
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory }
import spray.util._

import com.datastax.killrweather.Weather._

object DataIngestClientApp extends App with ClientHelper {
        
    implicit val system = ActorSystem("DataIngestClientApp")
    import system.dispatcher // execution context for futures
    val log = Logging(system, getClass)
    
    log.info("Loading raw weather data from file and posting as individual Json records to WeatherService ingest-api.")
    
    val config = ConfigFactory.load
    
    val targetUrl = config.getString("weatherservice.data.target.url")
    
    for {
        fs <- ingestData
        record <- fs.data
    } {
        val splitValues = Try(record.split(","))
            splitValues match {
                case Success(values) => postJson(values, targetUrl)
                case Failure(f) => 
                    log.info("csvFileToJsonIngest split error: " + f)
            }
    }
    
    private def postJson(attr: Array[String], targetUrl: String) {
        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
        
        Try(constructRawWeather(attr)) match {
            case Success(rwd) =>     
                val responseFuture: Future[HttpResponse] = 
                    pipeline(Post(targetUrl, rwd))
                    
                    responseFuture onComplete {
                        case Success(response) => // log.info("Success response: " + response)
                        case Failure(f) => log.error("Failed raw weather post: " + f)
                    }
                    
            case Failure(f) => 
                log.error("Failed to construct raw weather data from values: " + 
                    attr + "/nReason:/n" + f.getMessage)
        }
    }
    
    private def constructRawWeather(values: Array[String]) = {
        RawWeatherData(                    
            wsid = values(0),
            year = values(1).toInt,
            month = values(2).toInt,
            day = values(3).toInt,
            hour = values(4).toInt,
            temperature = values(5).toDouble,
            dewpoint = values(6).toDouble,
            pressure = values(7).toDouble,
            windDirection = values(8).toInt,
            windSpeed = values(9).toDouble,
            skyCondition = values(10).toInt,
            skyConditionText = values(11),
            oneHourPrecip = values(11).toDouble,
            sixHourPrecip = Option(values(12).toDouble).getOrElse(0)
        )
    }
    
    private def shutdown(): Unit = {
        IO(Http).ask(Http.CloseAll)(1.second).await
        system.shutdown()
    }
}
