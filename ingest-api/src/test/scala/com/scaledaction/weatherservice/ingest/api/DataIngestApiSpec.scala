package com.scaledaction.weatherservice.ingest.api

import akka.actor.{ Actor, Props }
import org.specs2.mutable.Specification
import spray.http._
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest
import StatusCodes._

class DataIngestApiSpec extends Specification with Specs2RouteTest with HttpService {
  def actorRefFactory = system

  val receiver = system.actorOf(Props((new Actor {
    def receive = {
      case _ => ()
    }
  })), "demo-service")

  val route = new DataIngestApi(receiver).route

  "DataIngestApi" should {

//    "return a greeting for GET requests to the root path" in {
//      Get() ~> route ~> check {
//        responseAs[String] must contain("Weather Service Data Ingest API")
//      }
//    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> route ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> sealRoute(route) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: POST"
      }
    }
  }
}
