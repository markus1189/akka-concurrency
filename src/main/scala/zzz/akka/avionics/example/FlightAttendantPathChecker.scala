package zzz.akka.avionics.example

import akka.actor.{ActorSystem, Props}

import zzz.akka.avionics.{LeadFlightAttendant, AttendantCreationPolicy}

object FlightAttendantPathChecker extends App {
  val system = akka.actor.ActorSystem("PlaneSimulation")
  val lead = system.actorOf(Props(
    new LeadFlightAttendant with AttendantCreationPolicy),
    "LeadFlightAttendant")

  Thread.sleep(2000)
  system.shutdown()
}
