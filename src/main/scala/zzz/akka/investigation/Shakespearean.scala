package zzz.akka.investigation

/**
* Why this is bad:
*
* Use of raw strings - easy to misspell
* Use of thread.sleep - how concurrent ist that
* Actor does not really do anything meaningful
*/

import akka.actor.{Actor, Props, ActorSystem}

class BadShakespeareanActor extends Actor {
  def receive = {
    case "Good Morning" => 
      println("Him: Forsooth 'tis the 'morn, but mourneth for thou doest I do!")
    case "You're terrible" =>
      println("Him: Yup")
  }
}                        

object ShakespeareanMain {
  val system = ActorSystem("BadShakespearean")
  val actor = system.actorOf(Props[BadShakespeareanActor], "Shake")

  def send(msg: String) {
    println(s"Me: $msg")
    actor ! msg
    Thread.sleep(100)
  }                  

  def main(args: Array[String]) {
    send("Good Morning")
    send("You're terrible")
    system.shutdown()
  }
}
