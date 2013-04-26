package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorSystem}
import akka.testkit.{TestKit, TestActorRef, ImplicitSender}
import org.scalatest.{FunSpec, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers

class TestEventSource extends Actor with ProductionEventSource {
  def receive = eventSourceReceive
}

class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec"))
                      with FunSpec
                      with ShouldMatchers
                      with BeforeAndAfterAll {

  import EventSource._

  override def afterAll() { system.shutdown() }

  describe("EventSource") {
    it("should allow us to register a listener") {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners should contain (testActor)
    }

    it("should allow us to unregister a listener") {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.receive(UnregisterListener(testActor))
      real.listeners should not (contain (testActor))
      real.listeners should be('empty)
    }

    it("should send the event to our test actor") {
      val testA = TestActorRef[TestEventSource]
      testA ! RegisterListener(testActor)
      testA.underlyingActor.sendEvent("Fibonacci")
      expectMsg("Fibonacci")
    }
  }
}
