package zzz.akka.avionics

import scala.collection.mutable.{ListBuffer,Buffer}
import akka.actor.{Actor,ActorRef}

object EventSource {
  case class RegisterListener(listener: ActorRef)
  case class UnregisterListener(listener: ActorRef)
}

trait EventSource { this: Actor =>
  def sendEvent[T](event: T): Unit
  def eventSourceReceive: Receive
}

trait ProductionEventSource extends EventSource { this: Actor =>
  import EventSource._

  val listeners = ListBuffer.empty[ActorRef]

  def sendEvent[T](event: T) { listeners foreach (_ ! event) }

  def eventSourceReceive: Receive = {
    case RegisterListener(listener) => listeners += listener
    case UnregisterListener(listener) => listeners -= listener
  }
}
