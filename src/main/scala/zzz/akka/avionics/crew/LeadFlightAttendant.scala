package zzz.akka.avionics.crew

import scala.util.Random
import scala.collection.JavaConverters._
import akka.actor.{Props, Actor, ActorRef}

trait AttendantCreationPolicy {
  val numberOfAttendants: Int = 8
  def createAttendant: Actor = FlightAttendant()
}

trait LeadFlightAttendantProvider {
  def newFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {
  case object GetFlightAttendant
  case class Attendant(a: ActorRef)
  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor {
  this: AttendantCreationPolicy =>

  import LeadFlightAttendant._

  override def preStart() {
    val attendantNames = context.system.settings.config.getStringList(
      "zzz.akka.avionics.flightcrew.attendantNames").asScala
    attendantNames take numberOfAttendants foreach { i =>
      context.actorOf(Props(createAttendant), i)
    }
  }

  def randomAttendant(): ActorRef = {
    context.children.take(
      Random.nextInt(numberOfAttendants) + 1).last
  }

  def receive = {
    case GetFlightAttendant =>
      sender ! Attendant(randomAttendant())
    case msg =>
      randomAttendant forward msg
  }

}
