package mkorolyov.model

case class LoadRatesRequest(
  isoCode: String,
  loadHistory: Option[Boolean] = None
)

object LoadRatesRequest {
  import play.api.libs.json.Json
  implicit lazy val formatter = Json.format[LoadRatesRequest]
}
