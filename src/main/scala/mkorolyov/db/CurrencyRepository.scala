package mkorolyov.db

import akka.stream.scaladsl.Source
import mkorolyov.entities.Rate
import mkorolyov.entities.Rate.Fields._
import mkorolyov.util.ExecutionContextProvider
import org.joda.time.DateTime
import play.api.libs.iteratee.Enumerator
import play.api.libs.streams.Streams
import reactivemongo.bson.{BSONDocument, Macros}
import reactivemongo.extensions.dao.BsonDao
import reactivemongo.extensions.dsl.BsonDsl

import scala.concurrent.Future
import scalaz._
import Scalaz._

trait CurrencyRepository {
  def update(rate: Rate): Unit

  def load(isoCode: String): Source[Rate, Unit]

  def last(isoCode: String): Future[Option[Rate]]
}

trait MongoCurrencyRespository extends CurrencyRepository with BsonDsl {
  self: ExecutionContextProvider ⇒

  private implicit val rateHandler = Macros.handler[Rate]

  private lazy val bsonDao = BsonDao[Rate, String](MongoContext.db, "currencies")

  private var lastRates: Map[String, Rate] = Map()

  override def update(rate: Rate): Unit = {
    last(rate.code)
      .filter(r ⇒ r.isEmpty || r.exists(_.rate != rate.rate))
      .map { _ ⇒
        lastRates += rate.code → rate
        bsonDao.insert(rate.copy(timestamp = DateTime.now.getMillis.some))
    }
  }

  override def load(isoCode: String): Source[Rate, Unit] = {
    val enumerator =
      bsonDao.collection
        .find(BSONDocument(fIsoCode → isoCode))
        .sort(BSONDocument(fTimestamp → -1))
        .cursor.enumerate().andThen(Enumerator.eof)

    Source(Streams.enumeratorToPublisher(enumerator))
  }

  def last(isoCode: String): Future[Option[Rate]] =
    OptionT(Future.successful(lastRates.get(isoCode)))
      .orElse(findLast(isoCode))
      .run

  private def findLast(isoCode: String): OptionT[Future, Rate] = {
    OptionT(
      bsonDao.find(
        selector = fIsoCode $eq isoCode,
        sort = fTimestamp $eq -1,
        page = 1,
        pageSize = 1
      ).map(_.headOption)
    )
  }

}
