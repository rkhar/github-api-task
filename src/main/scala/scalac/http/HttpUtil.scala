package scalac.http

import akka.http.scaladsl.model.HttpHeader

object HttpUtil {
  def extractHeaders(headers: Seq[HttpHeader]): Map[String, String] = headers.map { header =>
    header.lowercaseName() match {
      case h@"link" => (h, header.value())
      case h@_ => (h, header.value())
    }
  }.toMap

  def hasNextPage(headers: Map[String, String]): Boolean = headers.getOrElse("link", "").contains("rel=next")
}
