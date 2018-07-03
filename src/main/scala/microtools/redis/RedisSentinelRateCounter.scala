package microtools.redis

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.time.Duration

import java.util.concurrent.Executors
import microtools.actions.RateCounter
import play.api.inject.ApplicationLifecycle
import redis.clients.jedis.{JedisSentinelPool, Response}
import scala.collection.JavaConverters._
import resource._

import scala.concurrent.{ExecutionContext, Future}

class RedisSentinelRateCounter(sentinels: String, master: String, lifecycle: ApplicationLifecycle)
    extends RateCounter {

  lifecycle.addStopHook(() => this.stop())

  private val executor = Executors.newFixedThreadPool(
    4,
    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("jedis-pool-%d").build())
  private implicit val ec = ExecutionContext.fromExecutor(executor)

  private lazy val sentinelHosts: java.util.Set[String] =
    (sentinels.split(',').map(_.trim).toSet).asJava
  private lazy val jedisClient: JedisSentinelPool = new JedisSentinelPool(master, sentinelHosts)

  private def stop(): Future[Unit] = {
    executor.shutdown()
    jedisClient.destroy()
    Future.successful(())
  }

  override def incrementAndGet(key: String, storeFor: Duration): Future[Int] = {
    Future {
      managed(jedisClient.getResource()).acquireAndGet { jedis =>
        val transaction                             = jedis.multi()
        val countResponse: Response[java.lang.Long] = transaction.incr(key)
        transaction.expire(key, storeFor.getSeconds.toInt)
        transaction.exec()
        countResponse.get.toInt
      }
    }
  }
}
