package zzz.akka.avionics.controlling

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging}
import zzz.akka.avionics.EventSource
import zzz.akka.avionics.ProductionEventSource

object HeadingIndicator {
  case class BankChange(amount: Float)
  case class HeadingUpdate(heading: Float)

  def apply(): Actor = new HeadingIndicator with ProductionEventSource
}

class HeadingIndicator extends Actor with ActorLogging {
  this: EventSource =>

  import HeadingIndicator._
  import context._

  case object Tick

  val maxDegPerSec = 5
  val ticker = system.scheduler.schedule(
    100.millis,100.millis,self,Tick)

  var lastTick: Long = System.currentTimeMillis

  var rateOfBank = 0f

  var heading = 0f

  def headingIndicatorReceive: Receive = {
    case BankChange(amount) =>
      rateOfBank = amount.min(1.0f).max(-1.0f)
    case Tick =>
      val tick = System.currentTimeMillis
      val timeDelta = (tick - lastTick) / 1000f
      val degs = rateOfBank * maxDegPerSec
      heading = (heading + (360 + (timeDelta * degs))) % 360
      lastTick = tick
      sendEvent(HeadingUpdate(heading))
  }

  def receive = eventSourceReceive orElse headingIndicatorReceive

  override def postStop() { ticker.cancel() }
}

trait HeadingIndicatorProvider {
  def newHeadingIndicator: Actor = HeadingIndicator()
}
