package mkorolyov.http

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.ImplicitFlowMaterializer
import mkorolyov.Configuration
import mkorolyov.db.MongoDbKernel
import mkorolyov.http.CurrencyHttpEndpointActor.Start

class CurrencyHttpEndpointActor
  extends Actor
  with Directives
  with ImplicitFlowMaterializer
  with CurrencyHttpRoute
  with CurrencyService
  with MongoDbKernel {

  def receive = {
    case Start ⇒
      Http()(context.system)
        .bindAndHandle(route, Configuration.Endpoint.host, Configuration.Endpoint.port)
  }
}

object CurrencyHttpEndpointActor {
  case object Start
}