package microtools.filters.cors

import play.filters.cors.CORSConfig.Origins.Matching
import java.util.regex.Pattern

case class GlobbingMatching(allowedOrigins: Seq[String]) {

  val allowedOriginRegexes = allowedOrigins.map(globToRegex)

  private def globToRegex(glob: String): Pattern = {
    Pattern.compile(
      glob
        .map {
          _ match {
            case '*' => ".*"
            case '?' => "."
            case esc @ ('.' | '\\' | '+' | '(' | ')' | '|' | '^' | '$' | '@' | '%' | '{' | '[') =>
              "\\" + esc
            case char @ _ => char
          }
        }
        .mkString(""))
  }

  def matches(origin: String): Boolean = {
    allowedOriginRegexes.exists(p => p.matcher(origin).matches())
  }
  def matching(): Matching = {
    Matching(this.matches)
  }
}
