package scalac

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scalac.http.{HttpClient, HttpServer}

import scala.concurrent.ExecutionContext

object Boot {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "system")
  implicit val executionContext: ExecutionContext = system.executionContext

  val httpClient = new HttpClient()

  def main(args: Array[String]): Unit = {
    new HttpServer(httpClient).run()
  }

}
