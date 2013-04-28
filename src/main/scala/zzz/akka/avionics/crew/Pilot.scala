package zzz.akka.avionics.crew

import zzz.akka.avionics.{Plane, ControlSurfaces}
import akka.actor.{ActorRef, Actor}

object Pilots {
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot extends Actor {
  import Pilots._
  import Plane._
  import ControlSurfaces._

  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      context.parent ! Plane.GiveMeControl
      copilot = context.actorFor(s"../$copilotName")
      autopilot = context.actorFor("../AutoPilot")
    case controlSurfaces: ActorRef =>
      controls = controlSurfaces
  }
}

class CoPilot extends Actor {
  import Pilots._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor(s"../$pilotName")
      autopilot = context.actorFor("../AutoPilot")
  }
}

// TODO exercise for reader
class AutoPilot extends Actor {
  import Pilots._

  def receive = Actor.emptyBehavior
}

trait PilotProvider {
  def pilot: Actor = new Pilot
  def copilot: Actor = new CoPilot
  def autopilot: Actor = new AutoPilot
}
