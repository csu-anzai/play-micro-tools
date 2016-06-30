package microtools.ws

import java.net.URI
import java.time.Instant

import microtools.models.Problem
import org.asynchttpclient.cookie.DateParser
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.libs.ws.WSResponse

import scala.util.{Success, Try}

object WSResponseStatus {
  def unapply(response: WSResponse): Option[(Int, JsValue)] = {
    Some(response.status, response.json)
  }
}

object WSResponseOk {
  def unapply(response: WSResponse): Option[JsValue] = {
    if (response.status == Status.OK)
      Some(response.json)
    else
      None
  }
}

object WSResponseEmptyOk {
  def unapply(response: WSResponse): Boolean = response.status == Status.OK
}

object WSResponseOkWithExpires {
  def unapply(response: WSResponse): Option[(Option[Instant], JsValue)] = {
    if (response.status == Status.OK) {
      val expiresAt = response
        .header(HeaderNames.EXPIRES)
        .flatMap { expires =>
          Option(DateParser.parse(expires))
        }
        .map(_.toInstant)

      Some(expiresAt, response.json)
    } else {
      None
    }
  }
}

object WSResponseCreated {
  def unapply(response: WSResponse): Option[(URI, Option[JsValue])] = {
    if (response.status == Status.CREATED)
      response.header(HeaderNames.LOCATION).map { locationUrl =>
        val json =
          if (!response.bodyAsBytes.isEmpty)
            Some(response.json)
          else
            None
        (URI.create(locationUrl), json)
      } else
      None
  }
}

object WSResponseAccepted {
  def unapply(
      response: WSResponse): Option[(URI, Seq[String], Option[JsValue])] = {
    if (response.status == Status.ACCEPTED)
      response.header(HeaderNames.LOCATION).map { locationUrl =>
        val json =
          if (!response.bodyAsBytes.isEmpty)
            Some(response.json)
          else
            None
        (URI.create(locationUrl),
         response.allHeaders.getOrElse("link", Seq.empty),
         json)
      } else
      None
  }
}

object WSResponseNoContent {
  def unapply(response: WSResponse): Boolean =
    response.status == Status.NO_CONTENT
}

object WSResponseResetContent {
  def unapply(response: WSResponse): Boolean =
    response.status == Status.RESET_CONTENT
}

object WSResponseConflict {
  def unapply(response: WSResponse): Boolean =
    response.status == Status.CONFLICT
}

object WSResponseClientError {
  def unapply(response: WSResponse): Option[Problem] = {
    if (response.status >= Status.BAD_REQUEST &&
        response.status < Status.INTERNAL_SERVER_ERROR)
      Try(response.json).map(_.validate[Problem]) match {
        case Success(JsSuccess(problem, _)) => Some(problem)
        case _ =>
          Some(
              Problem
                .forStatus(response.status, response.statusText)
                .withDetails(response.body))
      } else
      None
  }
}

object WSResponseUnprocesssableEntity {
  def unapply(response: WSResponse): Option[Problem] = {
    if (response.status == Status.UNPROCESSABLE_ENTITY)
      Try(response.json).map(_.validate[Problem]) match {
        case Success(JsSuccess(problem, _)) => Some(problem)
        case _ =>
          Some(
              Problem
                .forStatus(response.status, response.statusText)
                .withDetails(response.body))
      } else
      None
  }
}

object WSResponseServerError {
  def unapply(response: WSResponse): Option[Problem] = {
    if (response.status >= Status.INTERNAL_SERVER_ERROR)
      Try(response.json).map(_.validate[Problem]) match {
        case Success(JsSuccess(problem, _)) => Some(problem)
        case _ =>
          Some(
              Problem
                .forStatus(response.status, response.statusText)
                .withDetails(response.body))
      } else
      None
  }
}
