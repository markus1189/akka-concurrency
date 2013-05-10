package zzz.akka.avionics.crew

import zzz.akka.avionics.{Plane, ControlSurfaces}
import akka.actor.{ActorRef, Actor, Terminated}

object Pilots {
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot( plane: ActorRef
           , autopilot: ActorRef
           , var controls: ActorRef
           , altimeter: ActorRef
           ) extends Actor {

  import Pilots._
  import Plane._
  import ControlSurfaces._

  var copilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      copilot = context.actorFor(s"../$copilotName")
    case controlSurfaces: ActorRef =>
      controls = controlSurfaces
  }
}

class CoPilot ( plane: ActorRef        
              , autopilot: ActorRef    
              , var controls: ActorRef 
              , altimeter: ActorRef    
              ) extends Actor {        

  import Pilots._
  import Plane._

  var pilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor(s"../$pilotName")
      context.watch(pilot)
    case Terminated(_) =>
      plane ! GiveMeControl
  }
}

// TODO exercise for reader
class AutoPilot extends Actor {
  import Pilots._

  def receive = Actor.emptyBehavior
}

trait PilotProvider {
  def newPilot( plane: ActorRef, autopilot: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor =
    new Pilot(plane,autopilot,controls,altimeter)

  def newCoPilot( plane: ActorRef, autopilot: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor =
    new CoPilot(plane,autopilot,controls,altimeter)

  def newAutoPilot: Actor = new AutoPilot
}
