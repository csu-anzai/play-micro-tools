package microtools.shapeless

import play.api.data.validation.ValidationError
import play.api.libs.json._
import shapeless.labelled.{FieldType, field}
import shapeless.{:+:, ::, CNil, Coproduct, Generic, HList, HNil, Inl, Inr, Lazy, Witness}

trait ShapelessObjectJson {
  implicit val hNilWrites: OWrites[HNil] = OWrites[HNil](_ => Json.obj())

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

  implicit val cNilReads: Reads[CNil] = Reads[CNil](_ => JsError(ValidationError("error.invalid")))

  implicit val cNilWrites: OWrites[CNil] = OWrites[CNil](_ => Json.obj())

  implicit def coproductWrites[H, T <: Coproduct](
      implicit hWrites: Lazy[OWrites[H]],
      tWrites: OWrites[T],
      namingStrategy: NamingStrategy,
      m: Manifest[H]
  ): OWrites[H :+: T] = OWrites[H :+: T] {
    case Inl(h) => hWrites.value.writes(h) ++ namingStrategy.nameFor(h)
    case Inr(t) => tWrites.writes(t)
  }

  implicit def coproductReads[H, T <: Coproduct](implicit hReads: Lazy[Reads[H]],
                                                 tReads: Reads[T],
                                                 namingStrategy: NamingStrategy,
                                                 m: Manifest[H]): Reads[H :+: T] = Reads[H :+: T] {
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

object ShapelessObjectJson extends ShapelessObjectJson
