package mkorolyov.service

import akka.actor.{PoisonPill, ActorLogging, Actor, ActorRef}
import akka.event.LoggingReceive
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import mkorolyov.entities.Rate
import mkorolyov.http.CurrencyLoaderActor.{RateResp, Load}
import scala.concurrent.duration._

class RateSourcePublisher(connector: ActorRef, isoCode: String)
  extends Actor
  with ActorLogging
  with ActorPublisher[Rate] {

  import context.dispatcher

  private val pubTimeout = 5 seconds

  context.system.scheduler.schedule(pubTimeout, pubTimeout, self, Publish)

  def receive = LoggingReceive {
    case Publish if isActive && totalDemand > 0 ⇒ connector ! Load(isoCode)
    case RateResp(Some(rate)) ⇒ onNext(rate)
    case Cancel ⇒ self ! PoisonPill
    case Request ⇒ //ignore
    case other ⇒
      println("mkorolyov.service.RateSourcePublisher")
      println(other)
      log.warning(s"received unhandler msg: $other")
  }

  case object Publish
}