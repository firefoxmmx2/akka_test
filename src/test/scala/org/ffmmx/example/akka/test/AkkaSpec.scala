package org.ffmmx.example.akka.test

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import org.ffmmx.example.akka.AkkaActorSum
import org.specs2.mutable.Specification
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, Duration}

/**
 * Created by hooxin on 15-4-3.
 */
object AkkaSpec extends Specification{
  "Akka Actor Sum " should {
    implicit val timeout = Timeout(Duration("5 seconds").toMillis)

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

}
