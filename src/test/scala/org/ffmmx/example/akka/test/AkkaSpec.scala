package org.ffmmx.example.akka.test


import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, SelectionKey, Selector, ServerSocketChannel}
import java.nio.charset.Charset

import akka.actor._
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import org.ffmmx.example.akka.{AkkaTest, AkkaQuery, AkkaActorSum, AkkaAgent}
import org.specs2.mutable.Specification
import org.specs2.reflect.ClassesOf
import org.specs2.time.NoTimeConversions

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by hooxin on 15-4-3.
 */
object AkkaSpec extends Specification with NoTimeConversions{
  implicit val timeout = Timeout(5 seconds)
  "Akka Actor Sum " should {

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
      val flr=futureList.map(_.sum)
      val rst=Await.result(flr,timeout.duration)
      (1 to 10).map(_ + 1).sum must be_==(rst)
    }
  }

  "Akka Actor" should {
    "DependencyInjector" in {
      val system = ActorSystem("mySystem")
      class DependencyInjector(applicationContext:AnyRef,beanName:String) extends IndirectActorProducer {
        def produce(): Actor = ???

        def actorClass: Class[_ <: Actor] = classOf[Actor]
      }
      val actorRef=system.actorOf(Props(classOf[DependencyInjector],system,"hello"),"HelloBean")
      1 must be_===(1)
    }

    "Muti Job Sum Task " in {
      case class HeartBeat(taskid:Int,parent:String)
      case class TaskFinished(taskid:Int)
      case object JobFinished
      case object JobStart
      case class TaskFailure(taskid:Int)
      case object TaskMessage
      case object TaskRestartMessage
      case object TaskFinishedMessage

      class Parent(jobid:String,tasknum:Int) extends Actor{
        val log=Logging(this.context.system,this)
        var tasks = IndexedSeq[ActorRef]()
        var replySender=this.context.system.deadLetters
        var count=0

        def receive: Actor.Receive = {
          case JobStart=>{
            this.replySender=this.context.sender()
            tasks=(1 to tasknum) map {
              id=>
                val actorRef=this.context.actorOf(Props(new Child(id)))
                tasks.foreach(actor=>(actor ! TaskMessage))
                actorRef
            }
          }
          case heartbeat:HeartBeat=>
            println("taskid-0000"+heartbeat.taskid+",finished:"+
            heartbeat.parent)
          case TaskFinished(taskid)=>
            println("taskid-0000"+taskid+" finished ... ")
            this.self ! TaskFinishedMessage

          case Terminated(actor)=>
            println(actor.path.toString+" stop...")
          case TaskFailure(taskid)=>
            val restartActor = this.context.actorOf(Props(new Child(tasks.length)))
            restartActor ! TaskRestartMessage
          case TaskFinishedMessage=>
            this.count+=1
            if(this.count == tasknum){
              this.replySender ! akka.actor.Status.Success("all task finished")
              println(this.count)
            }
        }
      }

      class Child(taskid:Int) extends Actor {
        val log=Logging(this.context.system,this)

        def receive: Actor.Receive = {
          case TaskMessage=>
            Thread.sleep(1000)
            this.context.parent ! HeartBeat(taskid,"10%")
            Thread.sleep(2000)

            //task failed
            this.context.stop(this.self)
            if(taskid%3==0){
              this.context.parent ! TaskFailure(this.taskid)
              log.info("taskid = "+taskid+" task failed..")
            }
            else {
              this.context.parent ! TaskFinished(this.taskid)
            }
          case TaskRestartMessage=>
            log.info(taskid+" restart...")
            this.context.parent ! TaskFinished(this.taskid)
        }
      }

      val system = ActorSystem("actorSystem")
      val jobActor=system.actorOf(Props(new Parent("DataSplitjob",10)),"DataSplitJob")
      val jobListener=jobActor ? JobStart
//      jobListener.onComplete {
//        case Success(result) =>
//          println("job finished ..., message : "+result)
//        case Failure(result)=>
//          println("job failed ... , message : "+result.getMessage)
//      }
      jobListener.onSuccess{
        case result=>
          println("job finished ..., message : "+result)
      }
      jobListener.onFailure{
        case result=>
          println("job failed ... , message : "+result.getMessage)
      }

      1 must be_===(1)
    }

    "Barista " in {
      sealed trait CoffeeRequest
      case object CappuccinoRequest extends CoffeeRequest
      case object EspressoRequest extends CoffeeRequest

      case class Bill(cents:Int)
      case object ClosingTime
      case object CaffeineWithdrawalWarning
      class Customer(caffeineSource:ActorRef) extends Actor {
        def receive: Actor.Receive = {
          case CaffeineWithdrawalWarning=>caffeineSource ! EspressoRequest
          case Bill(cents)=> println(s"I have to pay $cents cents, or else!")
        }
      }
      class Barista extends Actor {
        var cappuccinoCount=0
        var espressoCount=0

        def receive: Actor.Receive = {
          case CappuccinoRequest =>
            sender ! Bill(250)
            cappuccinoCount += 1
            println("I have to prepare a cappuccino!")
          case EspressoRequest =>
            sender ! Bill(200)
            espressoCount+=1
            println("Let's prepare on espresso.")
          case ClosingTime =>
            context.system.shutdown()
        }
      }

      val system=ActorSystem("actorSystem")
      val barista=system.actorOf(Props(new Barista),"Barista")
      val customer=system.actorOf(Props(new Customer(barista)),"Customer")
      barista!CappuccinoRequest
      barista!EspressoRequest
      println("I ordered a cappuccino and espresso")
      customer!CaffeineWithdrawalWarning
      barista!ClosingTime

      1 must be_===(1)
    }

    "Java Nio Socket" in {
      case class StartService(port:Int)
      class ServerActor extends Actor {
        val serverChannel=ServerSocketChannel.open()
        val serverSocket=serverChannel.socket()
        val selector= Selector.open()

        val buffer=ByteBuffer.allocate(1024)
        val charset=Charset.forName("utf8")
        val charDecoder=charset.newDecoder()

        def serverListenerStart(port:Int)={
          serverSocket.bind(new InetSocketAddress(port))
          serverChannel.configureBlocking(false)
          serverChannel.register(selector,SelectionKey.OP_ACCEPT)
          var n=0
          while(true){
            n=selector.select()
            if(n>0){
              val it=selector.selectedKeys().iterator()
              while(it.hasNext){
                val key=it.next()
                it.remove()
                if(key.isAcceptable){
                  val server=key.channel().asInstanceOf[ServerSocketChannel]
                  val channel=server.accept()
                  println(channel)
                  if(null != channel){
                    channel.configureBlocking(false)
                    channel.register(selector,SelectionKey.OP_READ)
                  }
                }
                else if (key.isReadable){
                  val socket=key.channel().asInstanceOf[SocketChannel]
                  var size:Int=0
                  println("read data .... "+ key)
                  buffer.clear()
                  size=socket.read(buffer)
                  while(size>0){
                    buffer.flip()
                    charDecoder.decode(buffer.asReadOnlyBuffer()).toString.split("""\.\.\.""").foreach(println)
                    buffer.clear()
                    size=socket.read(buffer)

                  }
                  if(-1 == size){ //当对端主动关闭后移除key,要不然selector会一直返回可读
                    socket.close()
                    selector.selectedKeys().remove(key)
                  }
                }
              }
            }
          }
        }
        def receive: Actor.Receive = {
          case StartService(port)=>
            serverListenerStart(port)
        }
      }

      class ClientActor extends Actor {
        val client=SocketChannel.open()
        val buffer=ByteBuffer.allocate(1024)

        def clientStart(port:Int) : Unit = {
          client.connect(new InetSocketAddress(port))
          while(true){
            for(i<- 1 to 5){
              buffer.clear()
              buffer.put((s"hello server $i ...").getBytes("utf8"))
              buffer.flip()
              client.write(buffer)
            }
            Thread.sleep(1000)
            println(System.currentTimeMillis()+"message to parent ...")
          }
        }
        def receive: Actor.Receive = {
          case StartService(port)=>
            clientStart(port)
        }
      }

      def akkaSocketTest(actorName:String,port:Int) = {
        val system=ActorSystem(actorName)
        val serverActor=system.actorOf(Props(new ServerActor),"serverActor")
        val clientActor=system.actorOf(Props(new ClientActor),"clientActor")
        val startCMD=new StartService(port)

        serverActor ! startCMD
        clientActor!startCMD
      }

//      akkaSocketTest("socketActor",11111)
      1 must be_===(1)
    }

    "akka tell ask send test" in {
      val rst=AkkaTest.creaete

      rst must be_===(2)
    }
  }

  "Future" should {
    " combo " in {
      val a=Future {
        (1 to 100).sum
      }
      val b=Future {
        (1000 to 10000).sum
      }

      val c = for{
        x<-a.mapTo[Int]
        y<-b.mapTo[Int]
      } yield x + y

      val result=Await.result(c,timeout.duration)
      (1 to 100).sum + (1000 to 10000).sum must be_===(result)
    }
  }
}
