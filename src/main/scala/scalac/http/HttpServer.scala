package scalac.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import org.slf4j.{Logger, LoggerFactory}
import scalac.domain.http.CacheSettings

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class HttpServer(host: String, port: Int, override val githubClient: GithubClient, override val cacheSettings: CacheSettings)
                (implicit val system: ActorSystem[Nothing], executionContext: ExecutionContext) extends HttpRoutes {
  override val log: Logger = LoggerFactory.getLogger(getClass)

  // start http server
  def run(): Unit = Http().newServerAt(host, port).bind(routes).onComplete {
    case Success(binding) =>
      log.info(s"http server is running on http://{}:{}/", binding.localAddress.getHostString, binding.localAddress.getPort)
    case Failure(exception) =>
      log.error(s"Failed to bind http server, terminating system", exception)
  }

}
