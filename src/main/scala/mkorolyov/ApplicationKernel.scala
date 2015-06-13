package mkorolyov

import akka.actor.{Props, ActorSystem}
import akka.kernel.Bootable
import mkorolyov.http.CurrencyHttpEndpointActor
import mkorolyov.http.CurrencyHttpEndpointActor.Start

trait Kernel extends Bootable {
  implicit val actorSystem: ActorSystem = ActorSystem("services")

  override def startup(): Unit = {
    actorSystem.actorOf(Props(new CurrencyHttpEndpointActor)) ! Start
  }

  override def shutdown(): Unit = {
    actorSystem.shutdown()
  }
}

object ApplicationKernel extends App with Kernel {
  startup()
}