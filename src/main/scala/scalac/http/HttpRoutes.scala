package scalac.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class HttpRoutes(override val httpClient: HttpClient) extends GithubRoutes {
  val routes: Route = {
    concat(
      healthCheck(),
      repos(),
      reposV2(),
      reposV3(),
      repoContributors(),
      repoContributorsV2(),
      repoContributorsV3(),
      orgContributors(),
      orgContributorsV2(),
      orgContributorsV3()
    )
  }

  def healthCheck(): Route = path("healthcheck") {
    get {
      complete("ok")
    }
  }

}
