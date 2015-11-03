package com.scaledaction.core.akka

import akka.actor.{ ActorSystem, ActorRefFactory, Actor, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http._
import spray.routing._
import akka.actor.ActorRef

// TODO - See spray.routing.SimpleRoutingApp
//github/spray/spray-template/src/main/scala/{Boot.scala, MyService.scala}
trait HttpServerApp extends HttpService {

  //  def apply(service: ActorRef, interface: String, port: Int, requestTimeout: String = "20 s")(implicit system: ActorSystem): Unit = {
  //    //implicit val serviceImplicit = service
  //    //implicit val timeout = Timeout(5.seconds)
  //    implicit val timeout = stringToDuration(requestTimeout)
  //    implicit val executionContext = system.dispatcher
  //    val response = IO(Http) ? Http.Bind(service, interface = interface, port = port)
  //    shutdownIfNotBound(response)
  //  }

  @volatile private[this] var _refFactory: Option[ActorRefFactory] = None

  implicit def actorRefFactory = _refFactory getOrElse sys.error(
    "Route creation is not fully supported before `startServer` has been called, " +
      "maybe you can turn your route definition into a `def` ?")

  //  def startServer(
  //    httpConfig: HttpServerConfig,
  //    serviceActorName: String = "service-actor",
  //    requestTimeout: String = "20 s")(route: ⇒ Route)(implicit system: ActorSystem): Unit = {
  //    startServer(httpConfig.host, httpConfig.port,      serviceActorName,      requestTimeout)(route)(system)
  //  }

  def startServer(
    interface: String,
    port: Int,
    serviceActorName: String = "service-actor",
    requestTimeout: String = "20 s")(route: ⇒ Route)(implicit system: ActorSystem): Unit = {
    //implicit val serviceImplicit = service
    //implicit val timeout = Timeout(5.seconds)
    implicit val timeout = stringToDuration(requestTimeout)
    implicit val executionContext = system.dispatcher

    val serviceActor = system.actorOf(
      props = Props {
        new Actor {
          _refFactory = Some(context)
          def receive = {
            val system = 0 // shadow implicit system
            runRoute(route)
          }
        }
      },
      name = serviceActorName)

    val response = IO(Http) ? Http.Bind(serviceActor, interface = interface, port = port)
    shutdownIfNotBound(response)
  }

  def startServer(
    httpConfig: HttpServerConfig)(serviceActor: ActorRef)(implicit system: ActorSystem): Unit = {
    //implicit val serviceImplicit = service
    //implicit val timeout = Timeout(5.seconds)
    implicit val timeout = stringToDuration("20 s")
    implicit val executionContext = system.dispatcher
    //
    //    val serviceActor = system.actorOf(
    //      props = Props {
    //        new Actor {
    //          _refFactory = Some(context)
    //          def receive = {
    //            val system = 0 // shadow implicit system
    //            runRoute(route)
    //          }
    //        }
    //      },
    //      name = serviceActorName)

    val response = IO(Http) ? Http.Bind(serviceActor, interface = httpConfig.host, port = httpConfig.port)
    shutdownIfNotBound(response)
  }

  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  def shutdownIfNotBound(f: Future[Any])(implicit system: ActorSystem, ec: ExecutionContext) = {
    f.mapTo[Http.Event].map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println(s"REST interface could not bind: ${cmd.failureMessage}, shutting down.")
        system.shutdown()
    }.recover {
      case e: Throwable =>
        println(s"Unexpected error binding to HTTP: ${e.getMessage}, shutting down.")
        system.shutdown()
    }
  }

  import scala.concurrent.duration._
  //  def requestTimeout(config: Config): Timeout = 
  //    val t = config.getString("spray.can.server.request-timeout")
  def stringToDuration(t: String): Timeout = {
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}