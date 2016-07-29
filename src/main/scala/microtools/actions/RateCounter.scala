package microtools.actions

import java.time.Duration

import scala.concurrent.Future

trait RateCounter {
  def incrementAndGet(key: String, storeFor: Duration): Future[Int]
}
