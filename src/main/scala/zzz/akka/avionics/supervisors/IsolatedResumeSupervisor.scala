package zzz.akka.avionics.supervisors

import scala.concurrent.duration._
import akka.actor.{ActorKilledException,ActorInitializationException}
import akka.actor.SupervisorStrategy.{Stop,Resume,Escalate}

abstract class IsolatedResumeSupervisor(
  maxNrRetries: Int = -1,
  withinTimeRange: Duration = Duration.Inf) extends IsolatedLifeCycleSupervisor {

  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }
}
