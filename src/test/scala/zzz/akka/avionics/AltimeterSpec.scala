package zzz.akka.avionics

import akka.actor.{Actor,ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestLatch, ImplicitSender}

import scala.concurrent.duration._
import scala.concurrent.Await

import org.scalatest.{FunSpec, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec"))
                    with ImplicitSender
                    with ShouldMatchers
                    with FunSpec
                    with BeforeAndAfterAll {

  import Altimeter._

  override def afterAll() { system.shutdown() }

  class Helper {
    object EventSourceSpy {
      val latch = TestLatch(1)
    }

    trait EventSourceSpy extends EventSource {
      def sendEvent[T](event: T) { EventSourceSpy.latch.countDown() }
      def eventSourceReceive = Actor.emptyBehavior
    }

    def slicedAltimeter = new Altimeter with EventSourceSpy

    def actor() = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a,a.underlyingActor)
    }
  }

  describe("Altimeter") {
    it("should record rate of climb changes") { new Helper {
      val (_,real) = actor()
      real.receive(RateChange(1))
      real.rateOfClimb should equal(real.maxRateOfClimb)
    } }

    it("should keep rate of climb changes within bounds") { new Helper {
      val (_,real) = actor()
      real.receive(RateChange(2))
      real.rateOfClimb should equal(real.maxRateOfClimb)
    } }

    it("should calculate altitude changes") { new Helper {
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1)
      fishForMessage() {
        case AltitudeUpdate(altitude) if altitude == 0f => false
        case AltitudeUpdate(altitude) => true
      }
    } }

    it("should send events") { new Helper {
      val (ref,_) = actor()
      Await.ready(EventSourceSpy.latch, 1.second)
      EventSourceSpy.latch.isOpen should be(true)
    } }
  }
}
