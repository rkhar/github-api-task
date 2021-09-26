package scalac.http

import akka.actor.typed.ActorSystem
import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.slf4j.{Logger, LoggerFactory}
import scalac.domain.github.{Contributor, Repo, User}
import scalac.http.HttpUtil.{extractHeaders, hasNextPage}

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class GithubClient(url: String, queueSize: Int, githubToken: String)(implicit system: ActorSystem[Nothing], executionContext: ExecutionContext) extends HttpClient(url, queueSize) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val headers = Seq(Authorization(HttpCredentials.createOAuth2BearerToken(githubToken)))

  def getRepos(org: String, page: Int = 1): Future[List[Repo]] = queueRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"/orgs/$org/repos").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
      headers
    )
  ).flatMap {
    case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[List[Repo]]
  }

  def getRepoContributors(org: String, repo: String, page: Int = 1): Future[List[Contributor]] = queueRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"/repos/$org/$repo/contributors").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
      headers
    )
  ).flatMap {
    case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[List[Contributor]]
  }

  val repos = List("json-tools", "lift-rest-demo", "mvn-repo", "akka-persistence-eventsourcing", "tricity-sug", "git-training-ug", "devoxx-android", "galaxy-gear2-tutorial", "warsjawa-2014", "reactive-rabbit", "rough-slick", "scala-slack-bot", "MFRC522-python", "recru-app", "scalac-branding-svg", "akka-message-visualization", "spark-kafka-avro", "scala-slack-bot-core", "reactive-slick", "websocket-akka-http", "cljs-on-gh-pages", "octopus-on-wire", "wsug-akka-websockets", "planet.clojure", "WhoSaidThat", "macro-fun", "docker-jira", "docker-java8", "docker-activator", "docker-postgres")

  def getOrganizationContributors(org: String): Future[List[User]] =
    for {
      repositories <- getRepos(org)
      contributors <- Future.sequence(repositories.map(repo => getRepoContributors(org, repo.name)))
    } yield contributors.flatten.groupBy(_.login).view.mapValues(_.map(_.contributions).sum).toMap.map(elem => User(elem._1, elem._2)).toList.sortBy(_.contributions)

  def getReposV2(org: String, page: Int = 1, list: List[Repo] = List.empty): Future[List[Repo]] = queueRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"/orgs/$org/repos").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
      headers
    )
  ).flatMap {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      if (!entity.isKnownEmpty())
        Unmarshal(entity).to[List[Repo]].flatMap { repositories =>
          log.info(s"repositories: $repositories")
          if (hasNextPage(extractHeaders(headers))) getReposV2(org, page + 1, repositories ::: list)
          else Future(repositories ::: list)
        }
      else
        Future(list)
  }

  def getRepoContributorsV2(org: String, repo: String, page: Int = 1, list: List[Contributor] = List.empty): Future[List[Contributor]] = queueRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"/repos/$org/$repo/contributors").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
      headers
    )
  ).flatMap {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      if (!entity.isKnownEmpty())
        Unmarshal(entity).to[List[Contributor]].flatMap { contributors =>
          log.info(s"contributors: $contributors")
          if (hasNextPage(extractHeaders(headers))) getRepoContributorsV2(org, repo, page + 1, contributors ::: list)
          else Future(contributors ::: list)
        }
      else
        Future(list)
  }

  def getOrganizationContributorsV2(org: String): Future[List[User]] =
    for {
      repositories <- getReposV2(org)
      contributors <- Future.sequence(repositories.map(repo => getRepoContributorsV2(org, repo.name)))
    } yield contributors.flatten.groupBy(_.login).view.mapValues(_.map(_.contributions).sum).toMap.map(elem => User(elem._1, elem._2)).toList.sortBy(_.contributions)

  @tailrec
  final def getReposV3(org: String, page: Int = 1, list: List[Repo] = List.empty): List[Repo] =
    Await.result(queueRequest(
      HttpRequest(
        HttpMethods.GET,
        Uri(s"/orgs/$org/repos").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
        headers
      )
    ).flatMap { case HttpResponse(StatusCodes.OK, _, entity, _) =>
      if (!entity.isKnownEmpty())
        Unmarshal(entity).to[List[Repo]]
      else
        Future(list)
    }, 10.second) match {
      case repositories if repositories.nonEmpty =>
        log.info(s"repositories: $repositories")
        getReposV3(org, page + 1, repositories ::: list)
      case repositories =>
        repositories ::: list
    }

  @tailrec
  final def getRepoContributorsV3(org: String, repo: String, page: Int = 1, list: List[Contributor] = List.empty): List[Contributor] =
    Await.result(queueRequest(
      HttpRequest(
        HttpMethods.GET,
        Uri(s"/repos/$org/$repo/contributors").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
        headers
      )
    ).flatMap { case HttpResponse(StatusCodes.OK, _, entity, _) =>
      if (!entity.isKnownEmpty())
        Unmarshal(entity).to[List[Contributor]]
      else Future(list)
    }, 10.second) match {
      case contributors if contributors.nonEmpty =>
        log.info(s"contributors: $contributors")
        getRepoContributorsV3(org, repo, page + 1, contributors ::: list)
      case contributors => contributors ::: list
    }

  def getOrganizationContributorsV3(org: String): List[User] = {
    for {
      repo <- getReposV3(org)
      //      _ = log.info(s"repo: $repo")
      contributor <- getRepoContributorsV3(org, repo.name).groupBy(_.login).view.mapValues(_.map(_.contributions).sum).toMap
      //      _ = log.info(s"contributor: ${contributor._1} -> ${contributor._2}")
    } yield User(contributor._1, contributor._2)
  }.sortBy(_.contributions)

}
