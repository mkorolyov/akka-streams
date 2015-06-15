package mkorolyov.http


import akka.actor.SupervisorStrategy.Resume
import akka.actor.{OneForOneStrategy, ActorLogging, Actor}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Source, ImplicitFlowMaterializer, Sink}
import akka.util.ByteString
import mkorolyov.db.MongoDbKernel
import mkorolyov.entities.Rate
import mkorolyov.http.CurrencyLoaderActor.{Update, Load}
import akka.pattern.pipe
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.duration._

import scalaz._
import Scalaz._

class CurrencyLoaderActor
  extends Actor
  with ActorLogging
  with ImplicitFlowMaterializer
  with MongoDbKernel {

  import context.dispatcher
  import context.system

  // load new rates evenry 10 mins.
  context.system.scheduler.schedule(10.seconds, 10.minutes, self, Update)

  override val supervisorStrategy = OneForOneStrategy() { case error => Resume }

  def receive = LoggingReceive {
    case Load ⇒ rates pipeTo sender
    case Update ⇒ rates
    case other ⇒ log.warning(s"received unhandled msg $other")
  }

  private def rates: Future[List[Rate]] = {
    val (_, resp) = Http().outgoingConnection("bitpay.com", 80)
      .runWith(
        source = Source(List(HttpRequest(HttpMethods.GET, Uri("/api/rates")))),
        sink = Sink.head[HttpResponse]
      )

    resp
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
      .map(_.decodeString("UTF-8") |> { str ⇒ if (str.isEmpty) None else Some(str) })
      .map(_.map(Json.parse).map(_.as[List[Rate]]).getOrElse(List.empty) <| {l ⇒
        l.foreach(currencyRepo.update)
      })
  }
}

object CurrencyLoaderActor {
  case object Load
  case object Update
}

/*
object ClientTest extends App {
  implicit val as = ActorSystem("test")
  implicit val fm = ActorFlowMaterializer.create(as)
  import as.dispatcher
  import scala.concurrent.duration._

  val (_, futureResp) = Http().outgoingConnection("bitpay.com", 80)
    .runWith(
      source = Source(List(HttpRequest(HttpMethods.GET, Uri("/api/rates")))),
      sink = Sink.head[HttpResponse]
    )

  val resp = Await.result(futureResp.flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)), 10.seconds)
  println(resp.decodeString("UTF-8"))
}*/
