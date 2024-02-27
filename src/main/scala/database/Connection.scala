package database

import com.typesafe.config.ConfigFactory
import scalikejdbc._

object Connection {
  // Load configurations from application.conf
  private val config = ConfigFactory.load()

  // Database configuration keys
  private val dbConfigKey = "db.default"
  private val driverKey = s"$dbConfigKey.driver"
  private val urlKey = s"$dbConfigKey.url"
  private val userKey = s"$dbConfigKey.user"
  private val passwordKey = s"$dbConfigKey.password"

  // Extracting database configurations
  private val driver = config.getString(driverKey)
  private val url = config.getString(urlKey)
  private val user = config.getString(userKey)
  private val password = config.getString(passwordKey)

  // Initialize database connection pool
  Class.forName(driver)
  ConnectionPool.singleton(url, user, password)

  // Provide DB session for executing queries
  val session: DBSession = AutoSession
}
