package scalac.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

abstract class HttpClient(url: String, queueSize: Int)(implicit system: ActorSystem[Nothing], executionContext: ExecutionContext) {
  val poolClientFlow: Flow[(HttpRequest, Promise[HttpResponse]), (Try[HttpResponse], Promise[HttpResponse]), Http.HostConnectionPool] =
    Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](url)

  val queue: SourceQueueWithComplete[(HttpRequest, Promise[HttpResponse])] =
    Source.queue[(HttpRequest, Promise[HttpResponse])](queueSize, OverflowStrategy.dropNew)
      .via(poolClientFlow)
      .to(Sink.foreach({
        case (Success(resp), p) => p.success(resp)
        case (Failure(e), p) => p.failure(e)
      }))
      .run()

  def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }


}
