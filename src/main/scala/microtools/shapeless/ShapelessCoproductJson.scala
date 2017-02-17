package microtools.shapeless

import play.api.libs.json.{JsNull, Reads, Writes}
import shapeless.{:+:, CNil, Coproduct, Inl, Inr, Lazy}

object ShapelessCoproductJson {
  implicit val cNilWrites: Writes[CNil] = Writes[CNil](_ => JsNull)

  implicit def coproductWrites[H, T <: Coproduct](
      implicit hWrites: Lazy[Writes[H]],
      tWrites: Writes[T]
  ): Writes[H :+: T] = Writes[H :+: T] {
    case Inl(h) => hWrites.value.writes(h)
    case Inr(t) => tWrites.writes(t)
  }

  implicit def coproductReadsHead[H](implicit hReads: Reads[H]): Reads[H :+: CNil] =
    hReads.map(v => Inl[H, CNil](v))

  implicit def coproductReads[H, T <: Coproduct](implicit hReads: Lazy[Reads[H]],
                                                 tReads: Reads[T]): Reads[H :+: T] =
    hReads.value.map[H :+: T](h => Inl[H, T](h)) orElse tReads.map[H :+: T](t => Inr[H, T](t))
}
