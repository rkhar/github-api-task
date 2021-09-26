package scalac.http

import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, extractUri}
import akka.http.scaladsl.server.ExceptionHandler
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.slf4j.Logger
import scalac.domain.CommonException

trait GithubHttpExceptionHandler {
  val log: Logger

  /**
   * custom exception handler gor github routes
   */
  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case exc: ClassNotFoundException =>
        extractUri { uri =>
          log.error(s"request to $uri was failed: ${exc.getMessage}")
          complete(HttpResponse(
            NotFound,
            entity = HttpEntity(ContentTypes.`application/json`, CommonException(NotFound.intValue, exc.getMessage).asJson.toString())))
        }

      case exc =>
        extractUri { uri =>
          log.error(s"request to $uri was failed: ${exc.getMessage}")
          complete(HttpResponse(
            InternalServerError,
            entity = HttpEntity(ContentTypes.`application/json`, CommonException(InternalServerError.intValue, exc.getMessage).asJson.toString())))
        }
    }
}
