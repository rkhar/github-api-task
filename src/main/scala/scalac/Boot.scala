package scalac

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{Config, ConfigFactory}
import scalac.http.{GithubClient, HttpServer}

import scala.concurrent.ExecutionContext

object Boot {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "system")
  implicit val executionContext: ExecutionContext = system.executionContext

  val config: Config = ConfigFactory.load()

  val githubUrl: String = config.getString("github.url")
  val githubSize: Int = config.getInt("github.queue_size")
  val githubToken: String = System.getenv(config.getString("github.token"))

  val host: String = config.getString("http-server.interface")
  val port: Int = config.getInt("http-server.port")

  val httpClient = new GithubClient(githubUrl, githubSize, githubToken)

  def main(args: Array[String]): Unit = {
    new HttpServer(httpClient, host, port).run()
  }

}
