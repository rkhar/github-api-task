package scalac.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class HttpServer(httpClient: HttpClient)(implicit actorSystem: ActorSystem[Nothing], executionContext: ExecutionContext) {
  val log: Logger = LoggerFactory.getLogger(getClass)
  val routes: Route = new HttpRoutes(httpClient).routes

  def run(): Unit = Http().newServerAt("localhost", 8080).bind(routes).onComplete {
    case Success(binding) =>
      log.info(s"http server is running on http://{}:{}/", binding.localAddress.getHostString, binding.localAddress.getPort)
    case Failure(exception) =>
      log.error(s"Failed to bind http server, terminating system", exception)
  }
}
