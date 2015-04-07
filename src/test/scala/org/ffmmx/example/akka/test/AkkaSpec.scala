package org.ffmmx.example.akka.test


import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.ffmmx.example.akka.{AkkaActorSum, AkkaAgent}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by hooxin on 15-4-3.
 */
object AkkaSpec extends Specification{
  this :  NoTimeConversions=>
  "Akka Actor Sum " should {
    implicit val timeout = Timeout(5,SECONDS)

    "fork task" in {
      val system=ActorSystem("myActorSystem")
      println("="*13)
      val sumActor=system.actorOf(props = Props[AkkaActorSum.Sum])
      println("+"*13)
      val x= sumActor ? (1,100)
      val y=sumActor ? (101,1000)
      val z=sumActor ? (1001,1000000)
      val rstFuture=for{
        a <- x.mapTo[Int]
        b <- y.mapTo[Int]
        c <- z.mapTo[Int]
      } yield a+b+c
      val result = Await.result(rstFuture,timeout.duration)
      println("Future result = "+result)
      system.shutdown()
      (1 to 1000000).sum must be_==(result)

    }

  }

  "Akka Agent" should {
    implicit val timeout=Timeout(5,SECONDS)
    "test1" in  {
      AkkaAgent.agentTest
      AkkaAgent.monadic

      1 must be_==(1)
    }
  }

}
