package net.liftweb.json.scalaz

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.JsonAST._

object JsonScalaz {
  trait JSONR[A] {
    // FIXME Validation or Either ?
//    def read(json: JValue): Validation[(JValue, Class[A]), A]
    def read(json: JValue): Validation[String, A]
  }

  trait JSONW[A] {
    def write(value: A): JValue
  }

  trait JSON[A] extends JSONR[A] with JSONW[A]

  implicit def boolJSON: JSON[Boolean] = new JSON[Boolean] {
    def read(json: JValue) = json match {
      case JBool(b) => success(b)
      case x => failure(x.toString)
    }

    def write(value: Boolean) = JBool(value)
  }

  implicit def intJSON: JSON[Int] = new JSON[Int] {
    def read(json: JValue) = json match {
      case JInt(x) => success(x.intValue)
      case x => failure(x.toString)
    }

    def write(value: Int) = JInt(BigInt(value))
  }

  implicit def longJSON: JSON[Long] = new JSON[Long] {
    def read(json: JValue) = json match {
      case JInt(x) => success(x.longValue)
      case x => failure(x.toString)
    }

    def write(value: Long) = JInt(BigInt(value))
  }

  implicit def doubleJSON: JSON[Double] = new JSON[Double] {
    def read(json: JValue) = json match {
      case JDouble(x) => success(x)
      case x => failure(x.toString)
    }

    def write(value: Double) = JDouble(value)
  }

  implicit def stringJSON: JSON[String] = new JSON[String] {
    def read(json: JValue) = json match {
      case JString(x) => success(x)
      case x => failure(x.toString)
    }

    def write(value: String) = JString(value)
  }

  implicit def bigintJSON: JSON[BigInt] = new JSON[BigInt] {
    def read(json: JValue) = json match {
      case JInt(x) => success(x)
      case x => failure(x.toString)
    }

    def write(value: BigInt) = JInt(value)
  }

  implicit def jvalueJSON: JSON[JValue] = new JSON[JValue] {
    def read(json: JValue) = success(json)
    def write(value: JValue) = value
  }

  // FIXME abstract for any Traversable?
  implicit def listJSON[A: JSON]: JSON[List[A]] = new JSON[List[A]] {
    def read(json: JValue) = json match {
      case JArray(xs) => 
        xs.map(fromJSON[A]).sequence[PartialApply1Of2[Validation, String]#Apply, A]
      case x => failure(x.toString)
    }

    def write(values: List[A]) = JArray(values.map(x => toJSON(x)))
  }

  implicit def optionJSON[A: JSON]: JSON[Option[A]] = new JSON[Option[A]] {
    def read(json: JValue) = json match {
      case JNothing | JNull => success(None)
      case x => fromJSON[A](x).map(some)
    }

    def write(value: Option[A]) = value.map(x => toJSON(x)).getOrElse(JNothing)
  }

  def fromJSON[A: JSONR](json: JValue) = implicitly[JSONR[A]].read(json)
  def toJSON[A: JSONW](value: A) = implicitly[JSONW[A]].write(value)

  def field[A: JSONR](json: JValue, name: String) = json match {
    case JObject(fs) => 
      fs.find(_.name == name)
        .map(f => implicitly[JSONR[A]].read(f.value))
        .getOrElse(failure("no such field '" + name + "'"))
    case x => failure(x.toString)
  }

  def makeObj(fields: Traversable[(String, JValue)]): JObject = 
    JObject(fields.toList.map { case (n, v) => JField(n, v) })

  // FIXME to get ridof explicit toJSON calls?
  // implicit def jsonw2jvalue[A: JSONW](a: A): JValue = a.toJSON
}
