package mkorolyov.db

import mkorolyov.Configuration
import mkorolyov.util.GlobalExecutionContext
import reactivemongo.api.{DB, MongoDriver}
import scala.concurrent.Await
import scala.concurrent.duration._

object MongoContext extends GlobalExecutionContext {
  val driver = new MongoDriver
  val connection = driver.connection(Configuration.Mongo.hosts)
  private implicit val finiteDuration: FiniteDuration = 10.seconds
  Await.result(connection.waitForPrimary, finiteDuration)
  def db: DB = connection("currency")
}
