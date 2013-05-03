package zzz.akka.avionics.supervisors

import akka.actor.{OneForOneStrategy, AllForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy.Decider
import scala.concurrent.duration._

trait SupervisionStrategyFactory {
  type SupervisionStrategyMaker = (Int,Duration) => Decider => SupervisorStrategy

  def makeStrategy: SupervisionStrategyMaker
}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy = OneForOneStrategy.apply
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy = AllForOneStrategy.apply
}
