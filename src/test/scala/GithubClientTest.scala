import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{Logger, LoggerFactory}
import scalac.http.{GithubClient, GithubRoutes}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

class GithubClientTest extends AnyWordSpec with ScalatestRouteTest with Matchers with ScalaFutures with GithubRoutes {

  val repositories = List("json-tools", "lift-rest-demo", "mvn-repo", "akka-persistence-eventsourcing", "tricity-sug", "git-training-ug", "devoxx-android", "galaxy-gear2-tutorial", "warsjawa-2014", "reactive-rabbit", "rough-slick", "scala-slack-bot", "MFRC522-python", "recru-app", "scalac-branding-svg", "akka-message-visualization", "spark-kafka-avro", "scala-slack-bot-core", "reactive-slick", "websocket-akka-http", "cljs-on-gh-pages", "octopus-on-wire", "wsug-akka-websockets", "planet.clojure", "WhoSaidThat", "macro-fun", "docker-jira", "docker-java8", "docker-activator", "docker-postgres")

  lazy val testKit: ActorTestKit = ActorTestKit.create()

  override val log: Logger = LoggerFactory.getLogger(getClass)

  implicit def typedSystem: ActorSystem[Void] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  implicit val ec: ExecutionContextExecutor = typedSystem.executionContext

  implicit def default(implicit system: ActorSystem[Nothing]): RouteTestTimeout = RouteTestTimeout(20.seconds)

  val config: Config = ConfigFactory.load()

  val githubUrl: String = config.getString("github.url")
  val githubSize: Int = config.getInt("github.queue_size")
  val githubToken: String = System.getenv(config.getString("github.token"))

  override val githubClient: GithubClient = new GithubClient(githubUrl, githubSize, githubToken)

  "github routes" should {
    "return ok" in {
      val request = HttpRequest(uri = "/hesp")

      request ~> githubRoutes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return first at most 100 repositories of scalaconsultants (GET /orgs/some_org/repos)" in {
      val request = HttpRequest(uri = "/orgs/scalaconsultants/repos")

      request ~> githubRoutes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return error 404 scalaconsultantz (GET /orgs/some_org/repos)" in {
      val request = HttpRequest(uri = "/orgs/scalaconsultantz/repos")

      request ~> githubRoutes ~> check {
        status should ===(StatusCodes.NotFound)

        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return first at most 100 contributors of scalaconsultants' repository: json-tools (GET repos/some_org/some_repo/contributors)" in {
      val request = HttpRequest(uri = "/repos/scalaconsultants/json-tools/contributors")

      request ~> githubRoutes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return 404 of scalaconsultants' repository: json-toolz (GET repos/some_org/some_repo/contributors)" in {
      val request = HttpRequest(uri = "/repos/scalaconsultants/json-toolz/contributors")

      request ~> githubRoutes ~> check {
        status should ===(StatusCodes.NotFound)

        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return first at most 100 contributors of first at most 100 organization's scalaconsultants repositories (GET orgs/some_org//contributors)" in {
      val request = HttpRequest(uri = "/orgs/scalaconsultants/contributors")

      request ~> githubRoutes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return 404 of organization's scalaconsultantz repositories (GET orgs/some_org//contributors)" in {
      val request = HttpRequest(uri = "/orgs/scalaconsultantz/contributors")

      request ~> githubRoutes ~> check {
        status should ===(StatusCodes.NotFound)

        contentType should ===(ContentTypes.`application/json`)
      }
    }

  }
}
