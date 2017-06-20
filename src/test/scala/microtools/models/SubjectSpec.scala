package microtools.models

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen, Properties}
import shapeless.{:+:, ::, CNil, Coproduct, Generic, HList, HNil, Inl, Inr, Lazy}

object SubjectSpec extends Properties("Subject") {
  object derive {
    implicit def hlistGen[H](implicit arb: Arbitrary[H]): Arbitrary[H :: HNil] =
      Arbitrary[H :: HNil](arb.arbitrary.map(_ :: HNil))

    implicit def anyValGen[S <: Subject, H <: HList](implicit gen: Generic.Aux[S, H],
                                                     arb: Arbitrary[H]): Arbitrary[S] =
      Arbitrary[S](arb.arbitrary.map(r => gen.from(r)))

    implicit def cnilGen[H](implicit hArb: Lazy[Arbitrary[H]]): Arbitrary[H :+: CNil] =
      Arbitrary[H :+: CNil](hArb.value.arbitrary.map(h => Inl(h)))

    implicit def coproductGen[H, T <: Coproduct](implicit hArb: Lazy[Arbitrary[H]],
                                                 tArb: Arbitrary[T]): Arbitrary[H :+: T] =
      Arbitrary[H :+: T](
        Gen.oneOf(hArb.value.arbitrary.map(h => Inl(h)), tArb.arbitrary.map(t => Inr(t))))

    def deriveGen[T, R](implicit gen: Generic.Aux[T, R], arb: Lazy[Arbitrary[R]]): Arbitrary[T] =
      Arbitrary[T](arb.value.arbitrary.map(r => gen.from(r)))

    val arb: Arbitrary[Subject] = deriveGen
  }

  implicit val arbSubject: Arbitrary[Subject] = derive.arb

  property("any subject can be serialized and deserialized") = forAll { expected: Subject =>
    val asString = expected.toString
    val actual   = Subject(asString)

    actual == expected
  }
}
