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

/**
 * github client is responsible for communicating through http with api.github.com
 */
class GithubClient(url: String, queueSize: Int, githubToken: String)(implicit system: ActorSystem[Nothing], executionContext: ExecutionContext) extends HttpClient(url, queueSize) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  // headers configuration for exceed rate limitation from 50 to 5000
  val headers = Seq(Authorization(HttpCredentials.createOAuth2BearerToken(githubToken)))

  // receive first 100 repositories of concrete organization
  def getRepos(org: String, page: Int = 1): Future[List[Repo]] = queueRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"/orgs/$org/repos").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
      headers
    )
  ).flatMap {
    case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[List[Repo]]
    case HttpResponse(StatusCodes.NotFound, _, _, _) => throw new ClassNotFoundException(s"for organization: $org no repositories were found")
  }

  // receive first 100 contributors of concrete organization's repository
  def getRepoContributors(org: String, repo: String, page: Int = 1): Future[List[Contributor]] = queueRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"/repos/$org/$repo/contributors").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
      headers
    )
  ).flatMap {
    case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[List[Contributor]]
    case HttpResponse(StatusCodes.NotFound, _, _, _) => throw new ClassNotFoundException(s"for organization: $org and repo: $repo no contributors were found")
  }

  // receive first 100 contributors of first 100 organization's repositories
  def getOrganizationContributors(org: String): Future[List[User]] =
    for {
      repositories <- getRepos(org)
      contributors <- Future.sequence(repositories.map(repo => getRepoContributors(org, repo.name)))
    } yield contributors.flatten.groupBy(_.login).view.mapValues(_.map(_.contributions).sum).toMap.map(elem => User(elem._1, elem._2)).toList.sortBy(_.contributions)

  // receive all repositories of concrete organization recursively
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
    case HttpResponse(StatusCodes.NotFound, _, _, _) => throw new ClassNotFoundException(s"for organization: $org no repositories were found")

  }

  // receive all contributors of concrete organization's repository recursively
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
    case HttpResponse(StatusCodes.NotFound, _, _, _) => throw new ClassNotFoundException(s"for organization: $org and repo: $repo no contributors were found")

  }

  // receive all contributors of concrete organization recursively
  def getOrganizationContributorsV2(org: String): Future[List[User]] =
    for {
      repositories <- getReposV2(org)
      contributors <- Future.sequence(repositories.map(repo => getRepoContributorsV2(org, repo.name)))
    } yield contributors.flatten.groupBy(_.login).view.mapValues(_.map(_.contributions).sum).toMap.map(elem => User(elem._1, elem._2)).toList.sortBy(_.contributions)

  // receive all repositories of concrete organization tail recursively
  @tailrec
  final def getReposV3(org: String, page: Int = 1, list: List[Repo] = List.empty): List[Repo] =
    Await.result(queueRequest(
      HttpRequest(
        HttpMethods.GET,
        Uri(s"/orgs/$org/repos").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
        headers
      )
    ).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        if (!entity.isKnownEmpty())
          Unmarshal(entity).to[List[Repo]]
        else
          Future(list)
      case HttpResponse(StatusCodes.NotFound, _, _, _) => throw new ClassNotFoundException(s"for organization: $org no repositories were found")

    }, 10.second) match {
      case repositories if repositories.nonEmpty =>
        log.info(s"repositories: $repositories")
        getReposV3(org, page + 1, repositories ::: list)
      case repositories =>
        repositories ::: list
    }

  // receive all contributors of concrete organization's repository tail recursively
  @tailrec
  final def getRepoContributorsV3(org: String, repo: String, page: Int = 1, list: List[Contributor] = List.empty): List[Contributor] =
    Await.result(queueRequest(
      HttpRequest(
        HttpMethods.GET,
        Uri(s"/repos/$org/$repo/contributors").withQuery(Query(Map("per_page" -> "100", "page" -> page.toString))),
        headers
      )
    ).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        if (!entity.isKnownEmpty())
          Unmarshal(entity).to[List[Contributor]]
        else Future(list)

      case HttpResponse(StatusCodes.NotFound, _, _, _) => throw new ClassNotFoundException(s"for organization: $org and repo: $repo no contributors were found")

    }, 10.second) match {
      case contributors if contributors.nonEmpty =>
        log.info(s"contributors: $contributors")
        getRepoContributorsV3(org, repo, page + 1, contributors ::: list)
      case contributors => contributors ::: list
    }

  // receive all contributors of concrete organization tail recursively
  def getOrganizationContributorsV3(org: String): List[User] = {
    for {
      repo <- getReposV3(org)
      //      _ = log.info(s"repo: $repo")
      contributor <- getRepoContributorsV3(org, repo.name).groupBy(_.login).view.mapValues(_.map(_.contributions).sum).toMap
      //      _ = log.info(s"contributor: ${contributor._1} -> ${contributor._2}")
    } yield User(contributor._1, contributor._2)
  }.sortBy(_.contributions)

}
