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
  def update(isoCode: String, value: Double): Unit

  def load(isoCode: String): Source[Rate, Unit]
}

trait MongoCurrencyRespository extends CurrencyRepository with BsonDsl {
  self: ExecutionContextProvider ⇒

  private implicit val rateHandler = Macros.handler[Rate]

  private lazy val bsonDao = BsonDao[Rate, String](MongoContext.db, "currencies")

  private var lastRates: Map[String, Double] = Map()

  override def update(isoCode: String, value: Double): Unit = {
    OptionT(Future.successful(lastRates.get(isoCode)))
      .orElse(OptionT(findLast(isoCode)))
      .filter(_ != value)
      .map { _ ⇒
      lastRates += isoCode → value
      bsonDao.insert(Rate(isoCode, value, DateTime.now.getMillis))
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

  private def findLast(isoCode: String): Future[Option[Double]] = {
    bsonDao.find(
      selector = fIsoCode $eq isoCode,
      sort = fTimestamp $eq -1,
      page = 1,
      pageSize = 1
    ).map(_.headOption.map(_.value))
  }
}
