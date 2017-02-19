package microtools.shapeless

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, Json, OWrites, Reads}
import shapeless.{:+:, CNil, Coproduct, Generic, Inl, Inr, Lazy}

trait ShapelessAlgebraicEnum {
  implicit val cNilReads: Reads[CNil] = Reads[CNil](_ => JsError(ValidationError("error.invalid")))

  implicit val cNilWrites: OWrites[CNil] = OWrites[CNil](_ => Json.obj())

  implicit def coproductWrites[H, T <: Coproduct](
      implicit hWrites: Lazy[OWrites[H]],
      tWrites: OWrites[T],
      namingStrategy: NamingStrategy[H]
  ): OWrites[H :+: T] = OWrites[H :+: T] {
    case Inl(h) => hWrites.value.writes(h) ++ namingStrategy.nameFor(h)
    case Inr(t) => tWrites.writes(t)
  }

  implicit def coproductReads[H, T <: Coproduct](
      implicit hReads: Lazy[Reads[H]],
      tReads: Reads[T],
      namingStrategy: NamingStrategy[H]): Reads[H :+: T] = Reads[H :+: T] {
    case json if namingStrategy.verify(json) => hReads.value.reads(json).map(h => Inl(h))
    case json                                => tReads.reads(json).map(t => Inr(t))
  }

  def deriveWrites[T, R](implicit gen: Generic.Aux[T, R], writes: Lazy[OWrites[R]]): OWrites[T] =
    OWrites[T] { obj =>
      writes.value.writes(gen.to(obj))
    }

  def deriveReads[T, R](implicit gen: Generic.Aux[T, R], reads: Lazy[Reads[R]]): Reads[T] =
    reads.value.map(gen.from)

}

object ShapelessAlgebraicEnum extends ShapelessAlgebraicEnum
