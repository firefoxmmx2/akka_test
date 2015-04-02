package test.resources.scala.org.ffmmx.example.akka.test

import akka.actor._
import akka.pattern.ask
import org.ffmmx.example.akka.AkkaActorSum
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration._



object AkkaSpec extends Specification{
  "Akka Actor Sum " should {
    val timeout = Duration("5 seconds")
    val system=ActorSystem("myActorSystem")
    "fork task" in {
      println("="*13)
      val sumActor=system.actorOf(props = Props[AkkaActorSum.Sum])
      val x= sumActor ? (1,100)
      val y=sumActor ? (101,1000)
      val z=sumActor ? (1001,1000000)

      val rstFuture=for{
        a <- x.mapTo[Int]
        b <- x.mapTo[Int]
        c <- x.mapTo[Int]
      } yield a+b+c
      val result = Await.result(rstFuture,timeout)
      println("Future result = "+result)
      (1 to 1000000).sum must be_==(result)
    }
    system.shutdown()
  }

}
