package microtools.filters
import akka.stream.Materializer
import play.api.http.MediaType
import play.filters.gzip.GzipFilter

object CommonGzipFilter {
  def gzipFilter(implicit mat: Materializer): GzipFilter = {
    val gzipWhiteList: Seq[MediaType] = Seq(
      MediaType("text", "html", Seq.empty),
      MediaType("application", "javascript", Seq.empty),
      MediaType("text", "css", Seq.empty),
      MediaType("image", "*", Seq.empty)
    )

    def matches(outgoing: MediaType, mask: MediaType): Boolean = {

      def capturedByMask(value: String, mask: String): Boolean = {
        mask == "*" || value.equalsIgnoreCase(mask)
      }

      capturedByMask(outgoing.mediaType, mask.mediaType) && capturedByMask(outgoing.mediaSubType,
                                                                           mask.mediaSubType)
    }
    new GzipFilter(shouldGzip = (_, response) =>
      response.body.contentType match {
        case Some(MediaType.parse(outgoing)) =>
          gzipWhiteList.exists(matches(outgoing, _))
        case _ => false // Fail closed (to not gziping), since whitelists are intentionally strict.
    })
  }
}
