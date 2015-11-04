package com.scaledaction.weatherservice.ingest.client

import akka.actor.ActorSystem
import akka.event.Logging
import java.io.{BufferedInputStream, FileInputStream, File => JFile}
import spray.http._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import com.typesafe.config.{ Config, ConfigFactory }

import com.datastax.killrweather.Weather._
import ClientHelper._

object DataIngestClientApp extends App {
    
    implicit val system = ActorSystem("DataIngestClientApp")
    import system.dispatcher // execution context for futures
    val log = Logging(system, getClass)
    
    log.info("Loading raw weather data from file and posting as individual Json records to WeatherService ingest-api.")
    
    val config = ConfigFactory.load
    Try(config.getString("weatherservice.data.load.path")) match {
        case Success(filePath) => csvFileToJsonIngest(filePath)
        case Failure(f) => log.error("Failed to get data file path: " + f)
    }
    
    def csvFileToJsonIngest(filePath: String) = {
        log.info("csvFileToJsonIngest, filePath: " + filePath)
        
        Try(FileSource(new JFile(filePath))) match {
            case Success(fs) => 
                for(record <- fs.data){
                    val splitValues = Try(record.split(","))
                    splitValues match {
                        case Success(values) => postJson(values)
                        case Failure(f) => 
                            log.info("csvFileToJsonIngest split error: " + f)
                    }
                }
            case Failure(f) => 
                log.error("Failed to open file: " + f)
        }
        
    }

    def postJson(attr: Array[String]) {
        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
        
        Try(constructRawWeather(attr)) match {
            case Success(rwd) =>     
                val responseFuture: Future[HttpResponse] = 
                    pipeline(Post("http://127.0.0.1:5000/weather/data/json", rwd))
                    // TODO: This url needs to come from config. 5000 is KW app.
                    
                    responseFuture onComplete {
                        case Success(response) => // log.info("Success response: " + response)
                        case Failure(f) => log.error("Failed raw weather post: " + f)
                    }
                    
            case Failure(f) => log.error("Failed to construct raw weather data from values: " + attr + "/n" + f)
        }
    }
    
    def constructRawWeather(values: Array[String]) = {
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
}





