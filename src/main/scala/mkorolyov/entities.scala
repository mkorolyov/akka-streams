package mkorolyov

import play.api.libs.json.Json

package object entities {
  case class Rate(code: String, name: String, rate: Double, timestamp: Option[Long] = None)
  object Rate {
    implicit lazy val formatter = Json.format[Rate]

    object Fields {
      val fIsoCode = "code"
      val fName = "name"
      val fValues = "rate"
      val fTimestamp = "timestamp"
    }

  }
}
