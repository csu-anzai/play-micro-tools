package microtools.metrics

import com.codahale.metrics.{MetricRegistry, Timer}
import microtools.BusinessTry
import microtools.logging.{LoggingContext, WithContextAwareLogger}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TimedCallsSpec extends PlaySpec with MockitoSugar with MustMatchers {
  "TimedCalls" should {

    "time successful future" in new Context {
      val timer = mock[Timer]
      when(registry.timer(anyString())).thenReturn(timer)
      val timerCtx = mock[Timer.Context]
      when(timer.time()).thenReturn(timerCtx)
      when(futureDelegate.apply()).thenReturn(Future.successful("OK"))

      await(underTest.futureCall()) mustBe "OK"

      verify(futureDelegate).apply()
      val nameCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(registry).timer(nameCaptor.capture())
      nameCaptor.getValue must endWith(".futureCall")
    }

    "time failed future" in new Context {
      val timer = mock[Timer]
      when(registry.timer(anyString())).thenReturn(timer)
      val timerCtx = mock[Timer.Context]
      when(timer.time()).thenReturn(timerCtx)
      when(futureDelegate.apply())
        .thenReturn(Future.failed(new RuntimeException("Poop")))

      intercept[RuntimeException] {
        await(underTest.futureCall())
      }

      verify(futureDelegate).apply()
      val nameCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(registry).timer(nameCaptor.capture())
      nameCaptor.getValue must endWith(".futureCall")
    }

    "time successful tries" in new Context {
      val timer = mock[Timer]
      when(registry.timer(anyString())).thenReturn(timer)
      val timerCtx = mock[Timer.Context]
      when(timer.time()).thenReturn(timerCtx)
      when(tryDelegate.apply()).thenReturn(BusinessTry.success("OK"))

      underTest.tryCall().awaitResult mustBe BusinessTry.success("OK")

      verify(tryDelegate).apply()
      val nameCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(registry).timer(nameCaptor.capture())
      nameCaptor.getValue must endWith(".tryCall")
    }
  }

  class Context {
    implicit val ctx   = LoggingContext.static()
    val registry       = mock[MetricRegistry]
    val futureDelegate = mock[() => Future[String]]
    val tryDelegate    = mock[() => BusinessTry[String]]

    val underTest = new TimedCalls with WithContextAwareLogger{
      def futureCall(): Future[String] = timeFuture("futureCall").apply {
        futureDelegate.apply()
      }

      def tryCall(): BusinessTry[String] = timeTry("tryCall").apply {
        tryDelegate.apply()
      }

      override def metricRegistry: MetricRegistry = registry
    }
  }
}
