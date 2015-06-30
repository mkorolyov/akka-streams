package mkorolyov.http


import akka.actor.SupervisorStrategy.Resume
import akka.actor.{OneForOneStrategy, ActorLogging, Actor}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Source, ImplicitMaterializer, Sink}
import akka.util.ByteString
import mkorolyov.db.MongoDbKernel
import mkorolyov.entities.Rate
import mkorolyov.http.CurrencyLoaderActor.{RateResp, Update, Load}
import akka.pattern.pipe
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.duration._

import scalaz._
import Scalaz._

//todo: move to Pinned dispatcher.
class CurrencyLoaderActor
  extends Actor
  with ActorLogging
  with ImplicitMaterializer
  with MongoDbKernel {

  import context.dispatcher
  import context.system

  //todo: move to config
  private val ratesRequestTimeout = 5.seconds

  private var rates: Future[Iterable[Rate]] = _

  override def preStart(): Unit = {
    super.preStart()
    rates = loadRates
  }

  context.system.scheduler.schedule(
    ratesRequestTimeout, ratesRequestTimeout, self, Update
  )

  override val supervisorStrategy = OneForOneStrategy() { case error => Resume }

  def receive = LoggingReceive {
    case Load(isoCode) ⇒
      rates.map(_.find(_.code == isoCode)).map(RateResp) pipeTo sender
    case Update ⇒ loadRates
    case other ⇒ log.warning(s"received unhandled msg $other")
  }

  private def loadRates = {
    val (_, resp) = Http().outgoingConnectionTls("bitpay.com", 443)
      .runWith(
        source = Source(List(HttpRequest(HttpMethods.GET, Uri("/api/rates")))
        ),
        sink = Sink.head[HttpResponse]
      )

    resp
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
      .map(_.decodeString("UTF-8") |> { str ⇒ if (str.isEmpty) None else Some(str) })
      .map(_.map(Json.parse).map(_.as[Iterable[Rate]]).getOrElse(List.empty) <| {l ⇒
        l.foreach(currencyRepo.update)
      })
  }
}

object CurrencyLoaderActor {
  case class Load(isoCode: String)
  case object Update

  case class RateResp(opt: Option[Rate])
}