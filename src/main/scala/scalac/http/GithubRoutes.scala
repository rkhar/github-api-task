package scalac.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

trait GithubRoutes {

  val githubClient: GithubClient

  val githubRoutes: Route =
    concat(
      repos(),
      repoContributors(),
      orgContributors(),
      pathPrefix("v2") {
        concat(
          reposV2(),
          repoContributorsV2(),
          orgContributorsV3()
        )
      },
      pathPrefix("v3") {
        concat(
          reposV3(),
          repoContributorsV3(),
          orgContributorsV3())
      }
    )

  def repos(): Route = path("orgs" / Segment / "repos") { orgName =>
    complete(githubClient.getRepos(orgName))
  }

  def reposV2(): Route = path("orgs" / Segment / "repos") { orgName =>
    complete(githubClient.getReposV2(orgName))
  }

  def reposV3(): Route = path("orgs" / Segment / "repos") { orgName =>
    complete(githubClient.getReposV3(orgName))
  }

  def repoContributors(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    complete(githubClient.getRepoContributors(owner, repo))
  }

  def repoContributorsV2(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    complete(githubClient.getRepoContributorsV2(owner, repo))
  }

  def repoContributorsV3(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    complete(githubClient.getRepoContributorsV3(owner, repo))
  }

  def orgContributors(): Route = path("orgs" / Segment / "contributors") { org =>
    complete(githubClient.getOrganizationContributors(org))
  }

  def orgContributorsV2(): Route = path("orgs" / Segment / "contributors") { org =>
    complete(githubClient.getOrganizationContributorsV2(org))
  }

  def orgContributorsV3(): Route = path("orgs" / Segment / "contributors") { org =>
    complete(githubClient.getOrganizationContributorsV3(org))
  }

}
