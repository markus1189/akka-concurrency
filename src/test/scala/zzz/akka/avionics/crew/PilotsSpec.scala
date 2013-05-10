package zzz.akka.avionics.crew

import zzz.akka.avionics.Plane
import zzz.akka.avionics.supervisors._

import akka.actor.{ActorSystem, Actor, ActorRef, Props, PoisonPill }
import akka.pattern.ask
import akka.testkit.{TestKit, ImplicitSender, TestProbe}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import scala.concurrent.Await
import scala.concurrent.duration._

class FakePilot extends Actor {
  override def receive = {
    case _ =>
  }
}

object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
    zzz.akka.avionics.flightcrew.copilotName = "$copilotName"
    zzz.akka.avionics.flightcrew.pilotName = "$pilotName""""
}

class PilotsSpec extends TestKit(ActorSystem("PilotsSpec",
        ConfigFactory.parseString(PilotsSpec.configStr)))
                 with ImplicitSender
                 with FunSpec
                 with ShouldMatchers {

  import PilotsSpec._
  import Plane._

  def nilActor: ActorRef = TestProbe().ref

  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilots/$copilotName"

  def setupPilotsHierarchy(): ActorRef = {
    implicit val askTimeout = Timeout(4.seconds)

    val a = system.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
          def childStarter() {
            context.actorOf(Props[FakePilot], pilotName)
            context.actorOf(Props(new CoPilot(testActor, nilActor, nilActor, nilActor)), copilotName)
          }
      }), "TestPilots")

    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)

    system.actorFor(copilotPath) ! Pilots.ReadyToGo

    a
  }

  describe("The Copilot") {
    it("takes control when the Pilot dies") {
      setupPilotsHierarchy()

      system.actorFor(pilotPath) ! PoisonPill

      expectMsg(GiveMeControl)

      lastSender should equal (system.actorFor(copilotPath))
    }
  }
}
