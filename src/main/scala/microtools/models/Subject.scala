package microtools.models

import play.api.libs.json._

import scala.reflect.ClassTag

sealed trait Subject extends Any

case class AdminSubject(adminUser: String) extends AnyVal with Subject {
  override def toString: String = s"admin/$adminUser"
}

object AdminSubject {
  implicit val customerSubjectFormat: Format[AdminSubject] =
    Subject.jsonFormat[AdminSubject]
}

case class EmployeeSubject(employeeId: String) extends AnyVal with Subject {
  override def toString: String = s"employee/$employeeId"
}

case class CustomerSubject(customerId: String) extends AnyVal with Subject {
  override def toString: String = s"customer/$customerId"
}

object CustomerSubject {
  implicit val customerSubjectFormat: Format[CustomerSubject] =
    Subject.jsonFormat[CustomerSubject]
}

case class ApiSubject(apiTokenLabel: String) extends AnyVal with Subject {
  override def toString: String = s"api/$apiTokenLabel"
}

object ApiSubject {
  implicit val apiSubjectFormat: Format[ApiSubject] =
    Subject.jsonFormat[ApiSubject]
}

case class ServiceSubject(serviceName: String) extends AnyVal with Subject {
  override def toString: String = s"service/$serviceName"
}

object ServiceSubject {
  implicit val customerSubjectFormat: Format[ServiceSubject] =
    Subject.jsonFormat[ServiceSubject]
}

case class GenericSubject(subject: String) extends AnyVal with Subject {
  override def toString: String = subject
}

object Subject {
  def apply(str: String): Subject = str match {
    case subject if subject.startsWith("employee/") => EmployeeSubject(subject.drop(9))
    case subject if subject.startsWith("customer/") => CustomerSubject(subject.drop(9))
    case subject if subject.startsWith("service/")  => ServiceSubject(subject.drop(8))
    case subject if subject.startsWith("admin/")    => AdminSubject(subject.drop(6))
    case subject if subject.startsWith("api/")      => ApiSubject(subject.drop(4))
    case subject                                    => GenericSubject(subject)
  }

  implicit val jsonReads: Reads[Subject] = __.read[String].map(apply)

  implicit val jsonWrites: Writes[Subject] = Writes[Subject](subject => JsString(subject.toString))

  private[models] implicit def jsonFormat[T <: Subject](
      implicit ClassTag: ClassTag[T]): Format[T] = {
    def reads: Reads[T] = {
      Subject.jsonReads.flatMap {
        case ClassTag(cs) => Reads.pure(cs)
        case s =>
          val name = ClassTag.runtimeClass.getSimpleName
          Reads(_ => {
            JsError(s"Expected $name but got $s")
          })
      }
    }

    Format(reads, jsonWrites)
  }
}
