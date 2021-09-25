package scalac.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

trait GithubRoutes {

  val httpClient: HttpClient

  def zen(): Route = path("zen") {
    complete(httpClient.getZen())
  }

  def org(): Route = path("orgs" / Segment) { orgName =>
    complete(httpClient.getOrg(orgName))
  }

  def repos(): Route = path("orgs" / Segment / "repos") { orgName =>
    complete(httpClient.getRepos(orgName))
  }

  def repoContributors(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    complete(httpClient.getRepoContributors(owner, repo))
  }

  def orgContributors(): Route = path("orgs" / Segment / "contributors") { org =>
    complete(httpClient.getOrganizationContributors(org))
  }

}
