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

import com.datastax.killrweather.Weather._
import ClientHelper._

object DataIngestClientApp extends App {
    
    implicit val system = ActorSystem("DataIngestClientApp")
    import system.dispatcher // execution context for futures
    val log = Logging(system, getClass)
    
    log.info("Loading raw weather data from file and posting as individual Json records to WeatherService ingest-api.")
    
    def csvFileToJsonIngest(filePath: String) = {
        val fs = FileSource(new JFile(filePath))
        val data = fs.data
        
        import scala.util.{Try, Success, Failure}
        
        for(record <- data){
            val splitValues = Try(record.split(","))
            splitValues match {
                case Success(values) => postJson(values)
                case Failure(f) => log.info("csvFileToJsonIngest split error: " + f)
            }
        }
    }

    def postJson(values: Array[String]) {
        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
        
        Try(constructRawWeather(values)) match {
            case Success(rwd) =>     
                val response: Future[HttpResponse] = 
                    pipeline(Get("http://127.0.0.1:8081/weather/data/json", rwd))
            case Failure(f) => log.error("Failed to construct raw weather data from values: " + values)
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





