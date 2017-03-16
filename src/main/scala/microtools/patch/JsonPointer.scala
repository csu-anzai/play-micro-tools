package microtools.patch

import play.api.libs.json._

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

/**
  * RFC 6901 json pointer to JsPath
  */
object JsonPointer {

  private object PathParser extends Parsers {
    type Elem = Char

    private val digit: Parser[Char] = elem("digit", _.isDigit)

    private val escapedSlash: Parser[Char] = '~' ~ '1' ^^ (_ => '/')

    private val escapedTilde: Parser[Char] = '~' ~ '0' ^^ (_ => '~')

    private val notSeparator: Parser[Char] = elem("notSeparator", _ != '/')

    private val number: Parser[PathNode] = digit.+ ^^ (chs => IdxPathNode(chs.mkString.toInt))

    private val string: Parser[PathNode] = (escapedSlash | escapedTilde | notSeparator).* ^^ (
        chs => KeyPathNode(chs.mkString))

    private val part: Parser[PathNode] = '/' ~> (number | string)

    private val pointer: Parser[JsPath] = part.* ^^ { parts =>
      JsPath(parts)
    }

    def parser: Parser[JsPath] = phrase(pointer)

    def apply(pointer: String): Either[String, JsPath] =
      parser(new CharSequenceReader(pointer)) match {
        case Success(path, _)  => Right(path)
        case NoSuccess(msg, _) => Left(msg)
      }
  }

  private val jsPathReads: Reads[JsPath] = implicitly[Reads[String]].flatMap { pointer =>
    PathParser(pointer) match {
      case Right(path) => Reads.pure(path)
      case Left(msg)   => Reads.apply(_ => JsError(s"Invalid json pointer: $msg"))
    }
  }

  private val jsPathWrites: Writes[JsPath] = {
    def escape(node: String) =
      node.replace("~", "~0").replace("/", "~1")

    Writes[JsPath](path =>
      JsString(path.path.map {
        case KeyPathNode(key)     => "/" + escape(key)
        case IdxPathNode(idx)     => "/" + idx.toString
        case RecursiveSearch(key) => "/" + escape(key)
      }.mkString))
  }

  implicit val jsPathFormat: Format[JsPath] = Format(jsPathReads, jsPathWrites)
}
