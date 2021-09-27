package scalac

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{Config, ConfigFactory}
import scalac.domain.http.CacheSettings
import scalac.http.{GithubClient, HttpServer}

import scala.concurrent.ExecutionContext

object Boot {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "system")
  implicit val executionContext: ExecutionContext = system.executionContext

  val config: Config = ConfigFactory.load()

  val githubUrl: String = config.getString("github.url")
  val githubSize: Int = config.getInt("github.queue_size")
  val githubToken: String = System.getenv(config.getString("github.token"))
  val githubCacheInitialCapacity: Int = config.getInt("github.cache-settings.initial-capacity")
  val githubCacheMaxCapacity: Int = config.getInt("github.cache-settings.max-capacity")
  val githubCacheTimeToLive: Int = config.getInt("github.cache-settings.time-to-live")
  val githubCacheTimeToIdle: Int = config.getInt("github.cache-settings.time-to-idle")
  val githubCacheSettings: CacheSettings = CacheSettings(githubCacheInitialCapacity, githubCacheMaxCapacity, githubCacheTimeToLive, githubCacheTimeToIdle)

  val host: String = config.getString("http-server.interface")
  val port: Int = config.getInt("http-server.port")

  val httpClient = new GithubClient(githubUrl, githubSize, githubToken)

  def main(args: Array[String]): Unit = {
    new HttpServer(host, port, httpClient, githubCacheSettings).run()
  }

}
