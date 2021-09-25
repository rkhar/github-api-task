package scalac.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class HttpRoutes(override val httpClient: HttpClient) extends GithubRoutes {
  val routes: Route = {
    concat(healthCheck(), zen(), org(), repos(), repoContributors(), orgContributors())
  }

  def healthCheck(): Route = path("healthcheck") {
    get {
      complete("ok")
    }
  }

}
