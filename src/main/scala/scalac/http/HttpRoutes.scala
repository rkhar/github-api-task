package scalac.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

trait HttpRoutes extends GithubRoutes {

  // main http routes
  val routes: Route =
    concat(
      healthCheck(),
      githubRoutes
    )

  def healthCheck(): Route = path("healthcheck") {
    get {
      complete("sup")
    }
  }

}
