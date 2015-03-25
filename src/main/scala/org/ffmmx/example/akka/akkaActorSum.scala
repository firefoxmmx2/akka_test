package org.ffmmx.example.akka

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
 * Created by hooxin on 15-3-25.
 */
object akkaActorSum {
  def doSum:Long = {
    0l
  }

  class Sum extends Actor {
    def receive: Receive = {
      case _ =>
    }
  }
}





