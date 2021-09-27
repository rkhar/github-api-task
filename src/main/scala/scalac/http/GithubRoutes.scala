package scalac.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, RouteResult}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import akka.http.caching.scaladsl.{Cache, CachingSettings, LfuCacheSettings}
import akka.http.caching.LfuCache
import akka.http.scaladsl.model.{HttpMethods, Uri}
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.directives.CachingDirectives._
import akka.http.caching.scaladsl.Cache
import akka.http.caching.scaladsl.CachingSettings
import akka.http.caching.LfuCache
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.directives.CachingDirectives._
import scala.concurrent.duration._
import scala.concurrent.duration._

trait GithubRoutes extends GithubHttpExceptionHandler {

  implicit val system: ActorSystem[Nothing]
  val githubClient: GithubClient

  /**
   * cache creating with lfu(Least Frequently Used) algorithm
   */
  val defaultCachingSettings: CachingSettings = CachingSettings(system)
  val lfuCacheSettings: LfuCacheSettings = //Minimum use of exclusion algorithm cache
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(128) //Starting unit
      .withMaxCapacity(1024) //Maximum Unit
      .withTimeToLive(1.hour) //Maximum retention time
      .withTimeToIdle(30.minutes) //Maximum Unused Time
  val cachingSettings: CachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  //Uri -> key, RouteResult -> value
  val lfuCache: Cache[Uri, RouteResult] = LfuCache[Uri, RouteResult](cachingSettings)

  //simple keyer for GET requests
  val simpleKeyer: PartialFunction[RequestContext, Uri] = {
    case request: RequestContext if request.request.method == HttpMethods.GET => request.request.uri
  }

  val githubRoutes: Route = Route.seal {
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
  }

  def repos(): Route = path("orgs" / Segment / "repos") { orgName =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getRepos(orgName)))
    }
  }

  def reposV2(): Route = path("orgs" / Segment / "repos") { orgName =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getReposV2(orgName)))
    }
  }

  def reposV3(): Route = path("orgs" / Segment / "repos") { orgName =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getReposV3(orgName)))
    }
  }

  def repoContributors(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getRepoContributors(owner, repo)))
    }
  }

  def repoContributorsV2(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getRepoContributorsV2(owner, repo)))
    }
  }

  def repoContributorsV3(): Route = path("repos" / Segment / Segment / "contributors") { (owner, repo) =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getRepoContributorsV3(owner, repo)))
    }
  }


  def orgContributors(): Route = path("orgs" / Segment / "contributors") { org =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getOrganizationContributors(org)))
    }
  }

  def orgContributorsV2(): Route = path("orgs" / Segment / "contributors") { org =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getOrganizationContributorsV2(org)))
    }
  }

  def orgContributorsV3(): Route = path("orgs" / Segment / "contributors") { org =>
    get {
      alwaysCache(lfuCache, simpleKeyer)(
        complete(githubClient.getOrganizationContributorsV3(org)))
    }
  }

}
