package microtools.shapeless

import play.api.libs.json._
import shapeless.labelled.{FieldType, field}
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, Lazy, Witness}

object ShapelessObjectJson {
  implicit val hNilWrites: OWrites[HNil] = OWrites[HNil](_ => Json.obj())

  implicit val cNilWrites: OWrites[CNil] = OWrites[CNil](_ => Json.obj())

  implicit val hNilReads: Reads[HNil] = Reads(_ => JsSuccess(HNil))

  implicit def hListObjectWrites[K <: Symbol, H, T <: HList](
      implicit witness: Witness.Aux[K],
      hWrites: Lazy[Writes[H]],
      tWrites: OWrites[T]
  ): OWrites[FieldType[K, H] :: T] = OWrites[FieldType[K, H] :: T] {
    case head :: tail =>
      val name = witness.value.name
      val h    = hWrites.value.writes(head)
      val t    = tWrites.writes(tail)

      Json.obj(name -> h) ++ t
  }

  implicit def coproductObjectWrites[H, T <: Coproduct](
      implicit hWrites: Lazy[OWrites[H]],
      tWrites: OWrites[T]
  ): OWrites[H :+: T] = OWrites[H :+: T] {
    case Inl(h) => hWrites.value.writes(h)
    case Inr(t) => tWrites.writes(t)
  }

  implicit def hListObjectReads[K <: Symbol, H, T <: HList](
      implicit witness: Witness.Aux[K],
      hReads: Lazy[Reads[H]],
      tReads: Reads[T]
  ): Reads[FieldType[K, H] :: T] = Reads[FieldType[K, H] :: T] { json =>
    ((json \ witness.value.name).validate(hReads.value), json.validate(tReads)) match {
      case (JsSuccess(h, _), JsSuccess(t, _))   => JsSuccess(field[K](h) :: t)
      case (JsError(errors1), JsError(errors2)) => JsError(errors1 ++ errors2)
      case (JsError(errors), _)                 => JsError(errors)
      case (_, JsError(errors))                 => JsError(errors)
    }
  }

}
