package mkorolyov.service

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.stream.scaladsl._
import akka.util.Timeout
import mkorolyov.db.DbKernel
import mkorolyov.entities.Rate
import mkorolyov.http.CurrencyLoaderActor
import play.api.libs.json.Json
import akka.pattern.ask
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait CurrencyService {
  self: DbKernel ⇒

  implicit val timeout: Timeout = 5 seconds

  val loader: ActorRef
  implicit val ec: ExecutionContext

  def actual: Flow[Message, Message, Unit] = {
    Flow[Message] map {
      case TextMessage.Strict(isoCode) if isoCode.nonEmpty ⇒
        import scalaz._
        import Scalaz._

        try {
          val actual = OptionT(
            (loader ? CurrencyLoaderActor.Load).mapTo[List[Rate]]
              .map(_.find(_.code == isoCode))
          ).orElse(OptionT(currencyRepo.last(isoCode)))
            .map(Json.toJson[Rate](_).toString)
            .getOrElse("[]")

          TextMessage.Streamed(akka.stream.scaladsl.Source(actual))
        } catch { case other: Throwable ⇒
          //todo: add logger
          println("mkorolyov.service.CurrencyService#actual")
          println(other.getMessage)
          println(other.getStackTrace.mkString("\n"))
          TextMessage.Strict(s"{}")
        }

      case other ⇒ TextMessage.Strict("unsupported request")
    }
  }

  def history: Flow[Message, Message, Unit] = {
    Flow[Message] map {
      case TextMessage.Strict(isoCode) if isoCode.nonEmpty ⇒
        val actual =
          Source((loader ? CurrencyLoaderActor.Load).mapTo[List[Rate]])
            .map(_.filter(_.code == isoCode))
        val histo = currencyRepo.load(isoCode).map(List(_))

        TextMessage.Streamed(
          (actual ++ histo).map(l ⇒ if (l.isEmpty) "" else Json.toJson(l).toString)
        )

      case other ⇒ TextMessage.Strict("unsupported request")
    }
  }
}
