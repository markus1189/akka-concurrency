package zzz.akka.avionics

import zzz.akka.avionics.crew.{ Pilots
                              , Pilot
                              , PilotProvider
                              , CoPilot
                              , AutoPilot
                              , LeadFlightAttendantProvider
                              , LeadFlightAttendant }

import akka.actor.{Actor, Props, ActorLogging, ActorRef}

object Plane {
  case object GiveMeControl
}

class Plane extends Actor with ActorLogging {
  this: AltimeterProvider
        with PilotProvider
        with LeadFlightAttendantProvider =>

  import Plane._
  import Altimeter._
  import EventSource._

  val cfgstr = "zzz.akka.avionics.flightcrew"
  val altimeter = context.actorOf(
    Props(Altimeter()),"Altimeter")

  val controls = context.actorOf(
    Props(new ControlSurfaces(altimeter)),"ControlSurfaces")

  val config = context.system.settings.config

  val pilot = context.actorOf(Props[Pilot],
    config.getString(s"$cfgstr.pilotName"))

  val copilot = context.actorOf(Props[CoPilot],
    config.getString(s"$cfgstr.copilotName"))

  val autopilot = context.actorOf(Props[AutoPilot],
    "Autopilot")

  val flightAttendant = context.actorOf(Props(LeadFlightAttendant()),
    config.getString(s"$cfgstr.leadAttendantName"))

  override def preStart() {
    altimeter ! RegisterListener(self) 
    Seq(pilot,copilot) foreach ( _ ! Pilots.ReadyToGo )
  }

  def receive = {
    case GiveMeControl =>
      log info("Plane giving control")
      sender ! controls
    case AltitudeUpdate(altitude) =>
      log info(s"Altitude is now: $altitude")
  }
}
