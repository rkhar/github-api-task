package scalac.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

trait GithubRoutes {

  val httpClient: HttpClient

  def repos(): Route = path("orgs" / Segment / "repos") { orgName =>
    complete(httpClient.getRepos(orgName))
  }

  def reposV2(): Route = path("v2" / "orgs" / Segment / "repos") { orgName =>
    complete(httpClient.getReposV2(orgName))
  }

  def reposV3(): Route = path("v3" / "orgs" / Segment / "repos") { orgName =>
    complete(httpClient.getReposV3(orgName))
  }

  def repoContributors(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    complete(httpClient.getRepoContributors(owner, repo))
  }

  def repoContributorsV2(): Route = path("v2" / "repos" / Segment / Segment / "contributors") { (owner, repo) =>
    complete(httpClient.getRepoContributorsV2(owner, repo))
  }

  def repoContributorsV3(): Route = path("v3" / "repos" / Segment / Segment / "contributors") { (owner, repo) =>
    complete(httpClient.getRepoContributorsV3(owner, repo))
  }

  def orgContributors(): Route = path("orgs" / Segment / "contributors") { org =>
    complete(httpClient.getOrganizationContributors(org))
  }

  def orgContributorsV2(): Route = path("v2" / "orgs" / Segment / "contributors") { org =>
    complete(httpClient.getOrganizationContributorsV2(org))
  }

  def orgContributorsV3(): Route = path("v3" / "orgs" / Segment / "contributors") { org =>
    complete(httpClient.getOrganizationContributorsV3(org))
  }

}
