package org.ffmmx.example.akka.test


import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import org.ffmmx.example.akka.{AkkaActorSum, AkkaAgent}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by hooxin on 15-4-3.
 */
object AkkaSpec extends Specification with NoTimeConversions{
  "Akka Actor Sum " should {
    implicit val timeout = Timeout(5 seconds)

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
    implicit val timeout=Timeout(5 seconds)
    "test1" in  {
      AkkaAgent.agentTest
      AkkaAgent.monadic

      1 must be_==(1)
    }
  }

  "Akka Future" should {
    implicit val timeout=Timeout(5 seconds)
    "sequence and traverse" in {
      val system=ActorSystem("mysystem")
      val actor=system.actorOf(Props(new Actor {
        def receive: Receive = {
          case s:Int => sender() ! s+1
          case _ =>

        }
      }))
      case class IntNumberBuilder(length:Int){
        var seq=(1 to length).toList

        def next:Int = {
          def snext(seqs:List[Int]) :Int = {
            seqs match {
              case head :: tails  =>
                seq = tails
                head
              case head :: Nil=>
                head
              case Nil =>
                0
            }
          }
          snext(seq)
        }

      }
      val intBuilder=IntNumberBuilder(10)
      val listOfFuture=List.fill(10)((actor ? intBuilder.next).mapTo[Int])
      val futureList=Future.sequence(listOfFuture)
      val flr=futureList.map(_.sum).mapTo[Int]
      val rst=Await.result(flr,timeout.duration)
      (1 to 10).map(_ + 1).sum must be_==(rst)
    }
  }
}
