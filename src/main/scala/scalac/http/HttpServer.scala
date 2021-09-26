package scalac.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class HttpServer(override val githubClient: GithubClient, host: String, port: Int)(implicit actorSystem: ActorSystem[Nothing], executionContext: ExecutionContext) extends HttpRoutes {
  override val log: Logger = LoggerFactory.getLogger(getClass)

  def run(): Unit = Http().newServerAt(host, port).bind(routes).onComplete {
    case Success(binding) =>
      log.info(s"http server is running on http://{}:{}/", binding.localAddress.getHostString, binding.localAddress.getPort)
    case Failure(exception) =>
      log.error(s"Failed to bind http server, terminating system", exception)
  }

}
