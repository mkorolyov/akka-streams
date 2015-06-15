package mkorolyov.http

import akka.actor.{Props, Actor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.ImplicitFlowMaterializer
import mkorolyov.Configuration
import mkorolyov.db.MongoDbKernel
import mkorolyov.http.CurrencyHttpEndpointActor.Start
import mkorolyov.service.CurrencyService

class CurrencyHttpEndpointActor
  extends Actor
  with Directives
  with ImplicitFlowMaterializer
  with CurrencyHttpRoute
  with CurrencyService
  with MongoDbKernel {

  override val ec = context.dispatcher

  val loader = context.actorOf(Props(new CurrencyLoaderActor))

  def receive = {
    case Start ⇒
      Http()(context.system)
        .bindAndHandle(route, Configuration.Endpoint.host, Configuration.Endpoint.port)
  }
}

object CurrencyHttpEndpointActor {
  case object Start
}