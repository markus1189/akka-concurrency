package zzz.akka.avionics.crew

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem, ActorRef}
import akka.testkit.{TestKit, TestActorRef, ImplicitSender}
import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers
import scala.concurrent.duration._

object TestFlightAttendant {
  def apply(time: Int) = new FlightAttendant with
    AttendantResponsiveness {
      val maxResponseTimeMS = time
    }
}

class FlightAttendantSpec extends TestKit(
                                    ActorSystem("FlightAttendantSpec",
                                    ConfigFactory.parseString(
                                      "akka.scheduler.tick-duration = 1 ms"))
                                  )
                          with ImplicitSender
                          with FunSpec
                          with ShouldMatchers {

  import FlightAttendant._

  def mkFlightAttendant(time: Int = 1): TestActorRef[FlightAttendant] = TestActorRef(Props(TestFlightAttendant(time)))

  describe("FlightAttendant") {
    it("gets a drink when asked") {
      val a = mkFlightAttendant(1)
      a ! GetDrink("Soda")
      expectMsg(Drink("Soda"))
    }

    it("is busy after getting a drink request") {
      val a = mkFlightAttendant(500)
      a ! GetDrink("Whatever")
      a ! Busy_?
      expectMsg(Yes)
      expectMsg(Drink("Whatever"))
    }

    it("delivers the correct drink") {
      val a = mkFlightAttendant(1)
      a ! GetDrink("Beer")
      expectMsg(Drink("Beer"))
    }
  }
}
