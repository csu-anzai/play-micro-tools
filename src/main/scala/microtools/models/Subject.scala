package microtools.models

import play.api.libs.json._

sealed trait Subject extends Any

case class AdminSubject(adminUser: String) extends AnyVal with Subject {
  override def toString: String = s"admin/$adminUser"
}

case class CustomerSubject(customerId: String) extends AnyVal with Subject {
  override def toString: String = s"customer/$customerId"
}

object CustomerSubject {
  implicit val customerSubjectFormat: Format[CustomerSubject] = Format(Subject.jsonReads.flatMap {
    case cs: CustomerSubject => Reads.pure(cs)
    case s                   => Reads(_ => JsError(s"Expected CustomerSubject but got $s"))
  }, Subject.jsonWrites)
}

case class CompanySubject(companyId: String) extends AnyVal with Subject {
  override def toString: String = s"company/$companyId"
}

case class ServiceSubject(serviceName: String) extends AnyVal with Subject {
  override def toString: String = s"service/$serviceName"
}

case class GenericSubject(subject: String) extends AnyVal with Subject {
  override def toString: String = subject
}

object Subject {
  def apply(str: String): Subject = str match {
    case subject if subject.startsWith("customer/") => CustomerSubject(subject.drop(9))
    case subject if subject.startsWith("service/")  => ServiceSubject(subject.drop(8))
    case subject if subject.startsWith("admin/")    => AdminSubject(subject.drop(6))
    case subject if subject.startsWith("company/")  => CompanySubject(subject.drop(8))
    case subject                                    => GenericSubject(subject)
  }

  implicit val jsonReads: Reads[Subject] = __.read[String].map(apply)

  implicit val jsonWrites: Writes[Subject] = Writes[Subject](subject => JsString(subject.toString))
}
