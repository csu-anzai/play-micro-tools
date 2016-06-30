package microtools.patch

import microtools.BusinessTry
import microtools.models.Problems
import play.api.libs.json.JsPath

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

/**
  * RFC 6901 json pointer to JsPath
  */
object JsonPointer extends Parsers {
  type Elem = Char

  val separator: Parser[Char] = elem('/')

  val digit: Parser[Char] = elem("digit", _.isDigit)

  val escapedSlash: Parser[Char] = elem('~') ~ elem('1') ^^ (_ => '/')

  val escapedTilde: Parser[Char] = elem('~') ~ elem('0') ^^ (_ => '~')

  val notSeparator: Parser[Char] = elem("notSeparator", _ != '/')

  val number: Parser[JsPath] = digit.+ ^^ (chs => JsPath(chs.mkString.toInt))

  val string: Parser[JsPath] = (escapedSlash | escapedTilde | notSeparator).* ^^ (chs =>
    JsPath() \ chs.mkString)

  val part: Parser[JsPath] = separator ~> (number | string)

  val pointer: Parser[JsPath] = part.* ^^ { parts =>
    parts.foldLeft(JsPath()) { (path, part) =>
      path.compose(part)
    }
  }

  def parser: Parser[JsPath] = phrase(pointer)

  def apply(pointer : String) : BusinessTry[JsPath] = parser(new CharSequenceReader(pointer)) match {
    case Success(path, _) => BusinessTry.success(path)
    case NoSuccess(msg, _) => BusinessTry.failure(Problems.BAD_REQUEST.withDetails(s"Invalid json pointer: $msg"))
  }
}
