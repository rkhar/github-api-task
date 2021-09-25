package scalac.http

import akka.actor.typed.ActorSystem
import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import org.slf4j.{Logger, LoggerFactory}
import scalac.domain.{Contributor, Repo, User}

import scala.concurrent.{ExecutionContext, Future}

class HttpClient()(implicit system: ActorSystem[Nothing], executionContext: ExecutionContext) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val headers = Seq(Authorization(HttpCredentials.createOAuth2BearerToken("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")))

  def getZen(): Future[String] = Http().singleRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri("https://api.github.com/zen")
    )
  ).flatMap(response => Unmarshal(response.entity).to[String])

  def getOrg(org: String): Future[Json] = Http().singleRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"https://api.github.com/orgs/$org")
    )
  ).flatMap(response => Unmarshal(response.entity).to[Json])

  def getRepos(org: String): Future[List[Repo]] = Http().singleRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"https://api.github.com/orgs/$org/repos"),
      headers
    )
  ).flatMap(response => Unmarshal(response.entity).to[List[Repo]])

  def getRepoContributors(owner: String, repo: String): Future[List[Contributor]] = Http().singleRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"https://api.github.com/repos/$owner/$repo/contributors"),
      headers
    )
  ).flatMap(response => Unmarshal(response.entity).to[List[Contributor]])

  def getUsername(login: String): Future[User] = Http().singleRequest(
    HttpRequest(
      HttpMethods.GET,
      Uri(s"https://api.github.com/users/$login"),
      headers
    )
  ).flatMap(response => Unmarshal(response.entity).to[User])

  val repos = List("json-tools", "lift-rest-demo", "mvn-repo", "akka-persistence-eventsourcing", "tricity-sug", "git-training-ug", "devoxx-android", "galaxy-gear2-tutorial", "warsjawa-2014", "reactive-rabbit", "rough-slick", "scala-slack-bot", "MFRC522-python", "recru-app", "scalac-branding-svg", "akka-message-visualization", "spark-kafka-avro", "scala-slack-bot-core", "reactive-slick", "websocket-akka-http", "cljs-on-gh-pages", "octopus-on-wire", "wsug-akka-websockets", "planet.clojure", "WhoSaidThat", "macro-fun", "docker-jira", "docker-java8", "docker-activator", "docker-postgres")

  def getOrganizationContributors(org: String): Future[List[Contributor]] = {
    val start = System.currentTimeMillis()

    val result = for {
      repositories <- getRepos(org)
      contributors <- Future.sequence(repositories.map(repo => getRepoContributors(org, repo.name)))
    } yield contributors.flatten.groupBy(_.login).view.mapValues(_.map(_.contributions).sum).toMap.map(elem => Contributor(elem._1, elem._2)).toList.sortBy(_.contributions)

    val finish = System.currentTimeMillis()

    log.info(s"time required: ${finish - start}")

    result
  }


}
