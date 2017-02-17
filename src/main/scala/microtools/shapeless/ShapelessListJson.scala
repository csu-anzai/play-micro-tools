package microtools.shapeless

import play.api.libs.json._
import shapeless.{::, HList, HNil, Lazy}

object ShapelessListJson {
  implicit val hNilWrites: Writes[HNil] = Writes[HNil](_ => JsNull)

  implicit val hNilReads: Reads[HNil] = Reads(_ => JsSuccess(HNil))

  implicit def hListArrayWrites[H, T <: HList](
      implicit hWrites: Lazy[Writes[H]],
      tWrites: Writes[T]
  ): Writes[H :: T] = Writes[H :: T] {
    case head :: tail =>
      val h = hWrites.value.writes(head)
      val t = tWrites.writes(tail)

      (h, t) match {
        case (h: JsValue, t: JsArray) => Json.arr(h) ++ t
        case (h: JsValue, JsNull)     => Json.arr(h)
        case _                        => Json.arr()
      }
  }

  implicit def hListArrayReads[H, T <: HList](
      implicit hReads: Lazy[Reads[H]],
      tReads: Reads[T]
  ): Reads[H :: T] = Reads[H :: T] { json =>
    (json.head.validate(hReads.value), json.tail.validate(tReads)) match {
      case (JsSuccess(h, _), JsSuccess(t, _))   => JsSuccess(h :: t)
      case (JsError(errors1), JsError(errors2)) => JsError(errors1 ++ errors2)
      case (JsError(errors), _)                 => JsError(errors)
      case (_, JsError(errors))                 => JsError(errors)
    }
  }
}
