package mkorolyov.http

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Flow
import mkorolyov.db.DbKernel
import play.api.libs.json.Json

trait CurrencyHttpRoute extends Directives { self: CurrencyService ⇒

  val route =
    path("currencies" / "histo") {
      decodeRequest {
        logRequestResult("currencies/histo") {
          get {
            handleWebsocketMessages(histo)
          }
        }
      }
    }
}

trait CurrencyService {
  self: DbKernel ⇒

  def histo: Flow[Message, Message, Unit] = {
    Flow[Message] map {
      case TextMessage.Strict(isoCode) if isoCode.nonEmpty ⇒
        TextMessage.Streamed(
          currencyRepo.load(isoCode)
            .map(rate ⇒ Json.toJson(rate).toString())
        )
      case _ => TextMessage.Strict("Not supported message type")
    }
  }
}