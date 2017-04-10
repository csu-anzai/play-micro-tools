package microtools.models

import org.scalacheck.{Arbitrary, Gen}

object Arbitraries {
  import org.scalacheck.Arbitrary._

  implicit def arbToken: Arbitrary[Token] = Arbitrary[Token](arbitrary[String].map(Token.apply))

  implicit def arbSubject: Arbitrary[Subject] =
    Arbitrary[Subject](
      Gen.oneOf(
        arbitrary[String].map(AdminSubject.apply),
        arbitrary[String].map(CustomerSubject.apply),
        arbitrary[String].map(CompanySubject.apply),
        arbitrary[String].map(AdminSubject.apply),
        arbitrary[String].map(GenericSubject.apply)
      ))
}
