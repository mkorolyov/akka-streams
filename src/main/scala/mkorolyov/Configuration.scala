package mkorolyov

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._

object Configuration {

  private val config = ConfigFactory.load()
  import config._

  object Mongo {
    private val conf = getConfig("mongo")
    val hosts = conf.getStringList("hosts").toList
  }

  object Endpoint {
    private val conf = getConfig("endpoint")
    val host = conf.getString("host")
    val port = conf.getInt("port")
  }

}
