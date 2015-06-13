package mkorolyov

import play.api.libs.json.Json

package object entities {
  case class Rate(isoCode: String, value: Double, timestamp: Long)
  object Rate {
    implicit lazy val formatter = Json.format[Rate]

    object Fields {
      val fIsoCode = "isoCode"
      val fValues = "value"
      val fTimestamp = "timestamp"
    }

  }
}
