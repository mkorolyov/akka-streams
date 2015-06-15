package mkorolyov.http

import akka.http.scaladsl.server.Directives
import mkorolyov.service.CurrencyService

trait CurrencyHttpRoute extends Directives { self: CurrencyService ⇒

  private val currencies = "currencies"
  private val histo = "histo"

  val route =
    decodeRequest {
      pathPrefix(currencies) {
        path(histo) {
          logRequestResult(s"$currencies/$histo") {
            get {
              handleWebsocketMessages(history)
            }
          }
        } ~ pathEnd {
          logRequestResult(currencies) {
            handleWebsocketMessages(actual)
          }
        }

      }
    }
}