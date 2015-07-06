package mkorolyov.service

import akka.actor._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl._
import mkorolyov.db.DbKernel
import mkorolyov.entities.Rate
import mkorolyov.model.LoadRatesRequest
import play.api.libs.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait CurrencyService {
  self: DbKernel ⇒

  val loader: ActorRef
  val as: ActorSystem
  implicit val ec: ExecutionContext

  //todo: add logger
  def loadRates(loader: ActorRef): Flow[Message, Message, Unit] = {
    Flow[Message] map {
      case TextMessage.Strict(req) if req.nonEmpty ⇒
        (Try(Json.parse(req).as[LoadRatesRequest]) match {
          case Failure(e) ⇒ println(e.getMessage); None
          case Success(obj) ⇒ Some(obj)
        }) match {
          case Some(request) ⇒
            val actualSource =
              Source.actorPublisher[Rate](
                Props(classOf[RateSourcePublisher], loader, request.isoCode)
              )

            val histoSource = request.loadHistory match {
              case Some(true) ⇒ currencyRepo.load(request.isoCode)
              case _ ⇒ Source.empty[Rate]
            }

            val resultSource =
              (histoSource ++ actualSource)
              .via(Flow[Rate].map(Json.toJson[Rate](_).toString))

            TextMessage(resultSource)

          case None ⇒ TextMessage.Strict("unsupported request")
        }

      case _ ⇒ TextMessage.Strict("unsupported request")
    }
  }

}