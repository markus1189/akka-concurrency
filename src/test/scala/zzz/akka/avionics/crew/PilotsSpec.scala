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

  def pilotPath(sys: String) = s"/user/$sys/$pilotName"
  def copilotPath(sys: String) = s"/user/$sys/$copilotName"
  def autopilotPath(sys: String) = s"/user/$sys/AutoPilot"

  def setupPilotsHierarchy(name: String): ActorRef = {
    implicit val askTimeout = Timeout(4.seconds)

    val a = system.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
          def childStarter() {
            context.actorOf(Props[FakePilot], pilotName)
            context.actorOf(Props(new CoPilot(testActor, nilActor, nilActor, nilActor)), copilotName)
            context.actorOf(Props(new AutoPilot(testActor)), "AutoPilot")
          }
      }), name)

    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)

    system.actorFor(copilotPath(name)) ! Pilots.ReadyToGo

    system.actorFor(autopilotPath(name)) !
      Plane.CoPilotReference(system.actorFor(copilotPath(name)))

    system.actorFor(autopilotPath(name)) !
      Plane.PilotReference(system.actorFor(pilotPath(name)))

    a
  }

  describe("The Copilot") {
    it("takes control if the pilot dies") {
      val sysName = "TestCoPilot"

      setupPilotsHierarchy(sysName)

      system.actorFor(pilotPath(sysName)) ! PoisonPill

      expectMsg(GiveMeControl)

      lastSender should equal (system.actorFor(copilotPath(sysName)))
    }
  }

  describe("The Autopilot") {
    it("takes control after death of the copilot") {
      val sysName = "TestAutoPilot"
      setupPilotsHierarchy(sysName)

      system.actorFor(pilotPath(sysName)) ! PoisonPill
      system.actorFor(copilotPath(sysName)) ! PoisonPill

      expectMsg(100.millis, GiveMeControl)

      lastSender should equal (system.actorFor(autopilotPath(sysName)))
    }

    it("does not care if the copilot dies while the pilot is alive") {
      val sysName = "TestAutoPilotNoTakeOver"
      setupPilotsHierarchy(sysName)

      system.actorFor(copilotPath(sysName)) ! PoisonPill

      expectNoMsg(750.millis)
    }
  }
}
