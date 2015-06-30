package mkorolyov.http

import akka.http.scaladsl.server.Directives
import mkorolyov.service.CurrencyService

trait CurrencyHttpRoute extends Directives { self: CurrencyService ⇒

  private val currencies = "currencies"

  val route =
    decodeRequest {
      path(currencies) {
        logRequestResult(currencies) {
          handleWebsocketMessages(loadRates(loader))
        }
      }
    }
}