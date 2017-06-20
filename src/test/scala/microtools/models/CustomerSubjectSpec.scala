package microtools.models

import org.scalacheck.Arbitrary.arbString
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Properties}
import org.scalatest.prop.Whenever
import play.api.libs.json.{JsString, Json}

class CustomerSubjectSpec extends Properties("CustomerProperties") with Whenever {
  implicit val arbCustomerSubject: Arbitrary[CustomerSubject] = Arbitrary(
    Arbitrary.arbString.arbitrary.map(CustomerSubject(_)))

  property("JSON serialization") = forAll { customerSubject: CustomerSubject =>
    val jsValue      = Json.toJson(customerSubject)
    val deserialized = jsValue.as[CustomerSubject]

    customerSubject == deserialized
  }

  /*property("Proper error messages for invalid JSON") = forAll { s: String =>
    whenever(!s.startsWith("customer")) {
      JsString(s).validate[CustomerSubject].isError
    }
  }*/
}
