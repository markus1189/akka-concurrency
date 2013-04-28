package zzz.akka.avionics

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestKit, TestActorRef, ImplicitSender}
import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

object TestFlightAttendant {
  def apply() = new FlightAttendant with
    AttendantResponsiveness {
      val maxResponseTimeMS = 1
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

  describe("FlightAttendant") {
    it("gets a drink when asked") {
      val a = TestActorRef(Props(TestFlightAttendant()))
      a ! GetDrink("Soda")
      expectMsg(Drink("Soda"))
    }
  }
}
