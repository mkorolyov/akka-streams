package mkorolyov.http

import akka.actor.{Props, Actor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.ImplicitMaterializer
import mkorolyov.Configuration
import mkorolyov.db.MongoDbKernel
import mkorolyov.http.CurrencyHttpEndpointActor.Start
import mkorolyov.service.CurrencyService

class CurrencyHttpEndpointActor
  extends Actor
  with Directives
  with ImplicitMaterializer
  with CurrencyHttpRoute
  with CurrencyService
  with MongoDbKernel {

  override val ec = context.dispatcher
  override val as = context.system

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