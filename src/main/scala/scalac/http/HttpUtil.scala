package scalac.http

import akka.http.scaladsl.model.HttpHeader

object HttpUtil {

  // extract headers from http responses
  def extractHeaders(headers: Seq[HttpHeader]): Map[String, String] = headers.map { header =>
    header.lowercaseName() match {
      case h@"link" => (h, header.value())
      case h@_ => (h, header.value())
    }
  }.toMap

  // find whether there is a next page or not. if header contains link with '...rel=next' return true
  def hasNextPage(headers: Map[String, String]): Boolean = headers.getOrElse("link", "").contains("rel=next")
}
