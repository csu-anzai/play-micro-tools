package microtools.shapeless

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsNull, Reads, Writes}
import shapeless.{:+:, CNil, Coproduct, Inl, Inr, Lazy}

trait ShapelessCoproductJson {
  implicit val cNilWrites: Writes[CNil] = Writes[CNil](_ => JsNull)

  implicit val cNilReads: Reads[CNil] = Reads[CNil]( _ => JsError(ValidationError("error.invalid")))

  implicit def coproductWrites[H, T <: Coproduct](
      implicit hWrites: Lazy[Writes[H]],
      tWrites: Writes[T]
  ): Writes[H :+: T] = Writes[H :+: T] {
    case Inl(h) => hWrites.value.writes(h)
    case Inr(t) => tWrites.writes(t)
  }

  implicit def coproductReads[H, T <: Coproduct](implicit hReads: Lazy[Reads[H]],
                                                 tReads: Reads[T]): Reads[H :+: T] =
    hReads.value.map[H :+: T](h => Inl[H, T](h)) orElse tReads.map[H :+: T](t => Inr[H, T](t))
}

object ShapelessCoproductJson extends ShapelessCoproductJson