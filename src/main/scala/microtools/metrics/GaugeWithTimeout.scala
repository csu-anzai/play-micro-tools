package microtools.metrics

import java.util.concurrent.atomic.AtomicReference

import com.codahale.metrics.Gauge

import scala.concurrent.duration.Duration

class GaugeWithTimeout[T](initialValue: T, timeoutValue: T, timeout: Duration) extends Gauge[T] {
  import GaugeWithTimeout._

  private val state: AtomicReference[State[T]] =
    new AtomicReference[State[T]](InitialState(initialValue))

  override def getValue: T = state.get().getValue

  def setValue(value: T): Unit =
    state.set(ValueState(System.currentTimeMillis() + timeout.toMillis, value, timeoutValue))
}

object GaugeWithTimeout {
  sealed trait State[T] {
    def getValue: T
  }

  case class InitialState[T](value: T) extends State[T] {
    override def getValue: T = value
  }

  case class ValueState[T](expiresAt: Long, value: T, timeoutValue: T) extends State[T] {
    override def getValue(): T =
      if (expiresAt < System.currentTimeMillis()) timeoutValue else value
  }
}
