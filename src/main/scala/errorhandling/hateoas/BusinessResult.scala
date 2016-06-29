package errorhandling.hateoas

import play.api.http.HeaderNames
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Result, Results}

trait BusinessResult {
  def asResult: Result
}

object BusinessResult {
  def ok[D](data: D,
            allowesActions: Seq[BusinessAction] = Seq.empty,
            etag: Option[String] = None)(implicit linkBuilder: LinkBuilder,
                                         writes: Writes[D]): BusinessResult =
    new BusinessResult {
      override def asResult: Result = {
        val headers = etag.map(HeaderNames.ETAG -> _).toSeq
        Results
          .Ok(JsonTransformers.addHAL(Json.toJson(data), allowesActions))
          .withHeaders(headers: _*)
      }
    }

  def created(selfAction: BusinessAction)(
      implicit linkBuilder: LinkBuilder): BusinessResult =
    new BusinessResult {
      override def asResult: Result = {
        val headers = Seq(
            HeaderNames.LOCATION -> linkBuilder.actionLink(selfAction).href)
        Results.Created.withHeaders(headers: _*)
      }
    }

  def created[D](selfAction: BusinessAction,
                 data: D,
                 allowesActions: Seq[BusinessAction] = Seq.empty)(
      implicit linkBuilder: LinkBuilder,
      writes: Writes[D]): BusinessResult =
    new BusinessResult {
      override def asResult: Result = {
        val headers = Seq(
            HeaderNames.LOCATION -> linkBuilder.actionLink(selfAction).href)
        Results
          .Created(JsonTransformers.addHAL(Json.toJson(data), allowesActions))
          .withHeaders(headers: _*)
      }
    }

  def deleted(): BusinessResult =
    new BusinessResult {
      override def asResult: Result = Results.NoContent
    }
}
