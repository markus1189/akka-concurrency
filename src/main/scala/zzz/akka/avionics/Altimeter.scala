package zzz.akka.avionics

import akka.actor.{Actor, Props, ActorSystem, ActorLogging}
import scala.concurrent.duration._

object Altimeter {
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)

  def apply() = new Altimeter with ProductionEventSource
}

class Altimeter extends Actor with ActorLogging  {
  this: EventSource =>

  import Altimeter._

  implicit val ec = context.dispatcher

  // maximum ceiling of plane in 'feet'
  val ceiling = 43000

  // maximum rate of climb in 'feet per minute'
  val maxRateOfClimb = 5000

  // varying rate of climb depending on movement of the stick
  var rateOfClimb: Float = 0

  var altitude: Double = 0

  // as time passes, we need to change the altitude based on the time passed.
  // the lastTick allows us to figure out how much time has passed
  var lastTick = System.currentTimeMillis

  // periodic ticker to tell us to update altitude
  val ticker = context.system.scheduler.schedule(100.millis, 100.millis, self, Tick)

  case object Tick

  def altimeterReceive: Receive = {
    case RateChange(amount) =>
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log info(s"Altimeter changed rate of climb to $rateOfClimb")
    case Tick =>
      val tick = System.currentTimeMillis
      altitude += ((tick -lastTick) / 60000.0) * rateOfClimb
      lastTick = tick
      sendEvent(AltitudeUpdate(altitude))
  }

  def receive = eventSourceReceive orElse altimeterReceive

  override def postStop() { ticker.cancel }
}
