package microtools.redis

import java.time.Duration

import akka.actor.ActorSystem
import microtools.actions.RateCounter
import redis.SentinelMonitoredRedisClientMasterSlaves

import scala.concurrent.{ExecutionContext, Future}

class RedisSentinelRateCounter(sentinels: String, master: String)(implicit ec: ExecutionContext,
                                                                  system: ActorSystem)
    extends RateCounter {
  import RedisSentinelRateCounter._

  lazy val client = SentinelMonitoredRedisClientMasterSlaves(
    sentinels.split(',').map(_.trim).map(parseSentinelHost),
    master)

  override def incrementAndGet(key: String, storeFor: Duration): Future[Int] = {
    try {
      val transaction = client.multi()
      val count       = transaction.incr(key)
      transaction.expire(key, storeFor.getSeconds)
      transaction.exec()

      count.map(_.toInt)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }
}

object RedisSentinelRateCounter {
  private def parseSentinelHost(str: String): (String, Int) =
    str.split(':').toList match {
      case host :: port :: Nil => (host, port.toInt)
      case host :: Nil         => (host, 26379)
      case _                   => throw new IllegalArgumentException(s"Not host:port : $str")
    }
}
