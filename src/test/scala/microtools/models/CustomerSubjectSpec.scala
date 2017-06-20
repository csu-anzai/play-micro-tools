package microtools.models

import org.scalacheck.Arbitrary.arbString
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.{Arbitrary, Properties}
import play.api.libs.json.{JsError, JsString, Json}

class CustomerSubjectSpec extends Properties("CustomerSubjectProperties") {
  implicit val arbCustomerSubject: Arbitrary[CustomerSubject] = Arbitrary(
    Arbitrary.arbString.arbitrary.map(CustomerSubject(_)))

  property("JSON format") = {
    Json.toJson(CustomerSubject("XYC3299")) == JsString("customer/XYC3299")
  }

  property("JSON serialization") = forAll { customerSubject: CustomerSubject =>
    val jsValue      = Json.toJson(customerSubject)
    val deserialized = jsValue.as[CustomerSubject]

    customerSubject == deserialized
  }

  property("Proper error messages for invalid JSON") = forAll { s: String =>
    (!s.startsWith("customer") && s.nonEmpty) ==> {
      val error = JsString(s).validate[CustomerSubject].asInstanceOf[JsError]
      error.toString contains "CustomerSubject"
    }
  }
}
