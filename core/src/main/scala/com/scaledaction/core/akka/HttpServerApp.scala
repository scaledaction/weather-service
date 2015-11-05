package com.scaledaction.core.akka

import akka.actor.{ ActorSystem, ActorRefFactory, Actor, Props }
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.http._
import spray.routing._
import akka.actor.ActorRef

// TODO - See spray.routing.SimpleRoutingApp
//github/spray/spray-template/src/main/scala/{Boot.scala, MyService.scala}
trait HttpServerApp extends HttpService with HasHttpServerConfig {

  @volatile private[this] var _refFactory: Option[ActorRefFactory] = None

  implicit def actorRefFactory = _refFactory getOrElse sys.error(
    "Route creation is not fully supported before `startServer` has been called, " +
      "maybe you can turn your route definition into a `def` ?")

  def startServer(route: ⇒ Route)(implicit system: ActorSystem): Unit = {
    startServer(getHttpServerConfig, route)(system)
  }

  def startServer(httpConfig: HttpServerConfig, route: ⇒ Route)(implicit system: ActorSystem): Unit = {

    val serviceActorName = "service-actor"

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

    startServer(httpConfig, serviceActor)(system)
  }

  def startServer(serviceActor: ActorRef)(implicit system: ActorSystem): Unit = {
    startServer(getHttpServerConfig, serviceActor)(system)
  }

  def startServer(httpConfig: HttpServerConfig, serviceActor: ActorRef)(implicit system: ActorSystem): Unit = {

    implicit val timeout = httpConfig.requestTimeout
    implicit val executionContext = system.dispatcher

    val response = IO(Http) ? Http.Bind(serviceActor, interface = httpConfig.host, port = httpConfig.port)
    shutdownIfNotBound(response)
  }

  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  private def shutdownIfNotBound(f: Future[Any])(implicit system: ActorSystem, ec: ExecutionContext) = {
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
}