package errorhandling

import play.api.mvc.Result

trait BusinessResult {
  def asResult : Result
}
