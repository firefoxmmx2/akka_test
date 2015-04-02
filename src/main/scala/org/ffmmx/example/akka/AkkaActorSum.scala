package org.ffmmx.example.akka

import akka.actor.Actor
import akka.actor.Actor.Receive

object AkkaActorSum {
  def doSum:Long = {
    0l
  }

  class Sum extends Actor {
    def receive: Receive = {
      case (from:Int,to:Int) =>
        val result=(from to to).foldLeft(0) { (x,y) => x+y }
        sender ! result
      case _ => sender ! 0
    }
  }
}





