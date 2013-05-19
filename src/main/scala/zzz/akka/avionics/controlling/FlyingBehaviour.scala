package zzz.akka.avionics.controlling

import scala.language.postfixOps

import scala.Function.const
import scala.concurrent.duration._

import akka.actor.{Actor,ActorRef,FSM}

import zzz.akka.avionics.{ControlSurfaces,Altimeter,Plane,EventSource}
import zzz.akka.avionics.crew.Pilots

import scalaz.Lens

object FlyingBehaviour {
  import ControlSurfaces._
  case object LostControl

  sealed trait State
  case object Idle extends State
  case object PreparingToFly extends State
  case object Flying extends State
  
  case class CourseTarget(
    altitude: Double,
    heading: Float,
    byMillis: Long)

  object CourseStatus {
    val altitude: Lens[CourseStatus,Double] = Lens.lensu(
      (cs,a) => cs.copy(altitude = a), _.altitude
    )

    val heading: Lens[CourseStatus, Float] = Lens.lensu(
      (cs,h) => cs.copy(heading = h), _.heading
    )

    val headingSinceMS: Lens[CourseStatus, Long] = Lens.lensu(
      (cs,hs) => cs.copy(headingSinceMS = hs), _.headingSinceMS
    )

    val altitudeSinceMS: Lens[CourseStatus, Long] = Lens.lensu(
      (cs,as) => cs.copy(altitudeSinceMS = as), _.altitudeSinceMS
    )
  }

  case class CourseStatus(
    altitude: Double,
    heading: Float,
    headingSinceMS: Long,
    altitudeSinceMS: Long)
         
  type Calculator = (CourseTarget, CourseStatus) => Any

  sealed trait Data
  case object Uninitialized extends Data

  object FlightData {
    val status: Lens[FlightData, CourseStatus] = Lens.lensu(
      (fd,st) => fd.copy(status = st), _.status
    )

    val heading: Lens[FlightData, Float] = 
      CourseStatus.heading compose status
    val headingSinceMS: Lens[FlightData, Long] = 
      CourseStatus.headingSinceMS compose status
    val altitude: Lens[FlightData, Double] =
      CourseStatus.altitude compose status
    val altitudeSinceMS: Lens[FlightData, Long] =
      CourseStatus.altitudeSinceMS compose status
    val controls: Lens[FlightData, ActorRef] = Lens.lensu(
      (fd,ctrls) => fd.copy(controls = ctrls),
      _.controls
    )

    def modifyHeadings(head: Float, ms: Long): FlightData => FlightData =
      (heading := head) flatMap (_ => headingSinceMS := ms) exec

    def modifyAltitude(alt: Double, ms: Long): FlightData => FlightData =
      (altitude := alt) flatMap (_ => altitudeSinceMS := ms) exec
  }

  case class FlightData(
    controls: ActorRef,
    elevCalc: Calculator,
    bankCalc: Calculator,
    target: CourseTarget,
    status: CourseStatus) extends Data

  case class Fly(target: CourseTarget)

  def currentMS = System.currentTimeMillis

  def calcElevator: Calculator = (target,status) => {
    val alt: Float = (target.altitude - status.altitude).toFloat
    val dur = target.byMillis - status.altitudeSinceMS
    if(alt < 0) StickForward((alt/dur) * -1)
    else StickBack(alt/dur)
  }

  def calcAilerons: Calculator = (target,status) => {
    import scala.math.{abs,signum}

    val diff = target.heading - status.heading
    val dur = target.byMillis - status.headingSinceMS
    val amount = if(abs(diff) < 180) diff
                 else signum(diff) * (abs(diff) - 360f)

    if (amount > 0) StickRight(amount / dur)
    else StickLeft((amount / dur) * -1)
  }
}

class FlyingBehaviour(
  plane: ActorRef,
  heading: ActorRef,
  altimeter: ActorRef) extends Actor 
with FSM[FlyingBehaviour.State,FlyingBehaviour.Data] {
  import FSM._
  import FlyingBehaviour._
  import Pilots._
  import Plane._
  import Altimeter._
  import HeadingIndicator._
  import EventSource._

  case object Adjust

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Fly(target), _) =>
      goto(PreparingToFly) using FlightData(
        context.system.deadLetters,
        calcElevator,
        calcAilerons,
        target,
        CourseStatus(-1,-1,0,0))
  }

  onTransition {
    case Idle -> PreparingToFly =>
      plane ! GiveMeControl
      heading ! RegisterListener(self)
      altimeter ! RegisterListener(self)
  }

    import FlightData._
  when(PreparingToFly, stateTimeout = 5.seconds)(transform {
    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using modifyHeadings(head,currentMS)(d)
    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using modifyAltitude(alt, currentMS)(d)
    case Event(Controls(ctrls), d: FlightData) =>
      stay using (FlightData.controls := ctrls).exec(d)
    case Event(StateTimeout, _) =>
      plane ! LostControl
      goto(Idle)
  } using {
    case s if prepComplete(s.stateData) =>
      s.copy(stateName = Flying)
  })

  def prepComplete(data: Data): Boolean = {
    data match {
      case FlightData(c,_,_,_,s) =>
        !c.isTerminated && s.heading != -1f && s.altitude != -1f
      case _ => false
    }
  }
}
