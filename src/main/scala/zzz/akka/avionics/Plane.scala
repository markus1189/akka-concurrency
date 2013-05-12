package zzz.akka.avionics

import zzz.akka.avionics.crew.{ Pilots
                              , Pilot
                              , PilotProvider
                              , CoPilot
                              , AutoPilot
                              , LeadFlightAttendantProvider
                              , LeadFlightAttendant }

import zzz.akka.avionics.supervisors._

import akka.pattern.ask
import akka.actor.{Actor, Props, ActorLogging, ActorRef}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

object Plane {
  case object GiveMeControl
  case object RequestCoPilot
  case class CoPilotReference(reference: ActorRef)

  def apply() = new Plane with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider
}

class Plane extends Actor with ActorLogging {
  this: AltimeterProvider with PilotProvider with LeadFlightAttendantProvider =>

  import Plane._
  import Altimeter._
  import EventSource._
  import IsolatedLifeCycleSupervisor._

  implicit val askTimeout = Timeout(1.second)

  val cfgstr = "zzz.akka.avionics.flightcrew"

  val config = context.system.settings.config

  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val leadAttendantName = config.getString(s"$cfgstr.leadAttendantName")

  def actorForControls(name: String) = context.actorFor("Equipment/" + name)
  def actorForPilots(name: String) = context.actorFor("Pilots/" + name)

  def startEquipment() {
    val plane = self

    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          context.actorOf(Props(newAutoPilot(plane)), "AutoPilot")
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
      }), "Equipment")

    Await.result(controls ? WaitForStart, 1.second)
  }

  def startPeople() {
    val plane = self

    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("AutoPilot")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
          context.actorOf(Props(newCoPilot(plane, autopilot, controls, altimeter)), copilotName)
        }
      }), "Pilots")

    context.actorOf(Props(newFlightAttendant), leadAttendantName)

    Await.result(people ? WaitForStart, 1.second)
  }

  override def preStart() { 
    import EventSource.RegisterListener
    import Pilots.ReadyToGo

    startEquipment()
    startPeople()
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForControls("AutoPilot") ! ReadyToGo
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
  }

  def receive = {
    case GiveMeControl =>
      log info("Plane giving control")
      sender ! actorForControls("ControlSurfaces")
    case AltitudeUpdate(altitude) =>
      log info(s"Altitude is now: $altitude")
    case RequestCoPilot =>
      sender ! CoPilotReference(actorForPilots(copilotName))
  }
}
