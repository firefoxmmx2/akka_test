import akka.actor._
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.Await

/**
 * Created with IntelliJ IDEA.
 * User: hooxin
 * Date: 13-12-2
 * Time: 下午9:24
 * To change this template use File | Settings | File Templates.
 */
object AkkaTest extends App {
  val system=ActorSystem("myakkaSys")
  val manager=system.actorOf(Props[MyAkkaActorManager],"myakkamanager")
  manager!Start

}

class MyAkkaActorManager extends Actor with ActorLogging {
  override def preStart(): Unit = {
    log.info("MyAkkaActorManager is prestart.")
    log.info("MyAkkaActor is starting..")
    context.actorOf(Props[MyAkkaActor],"myakka")
    log.info("MyAkkaActor is started")
  }

  def receive: Actor.Receive = {
    case Start => context.actorFor("/user/myakkamanager/myakka")!Done
    case Done =>
      log.info("MyAkkaActorManager receive 'Done'")
      log.info("MyAkkaActor is stopping..")
      context.stop(context.actorFor("/user/myakkamanager/myakka"))
      log.info("MyAkkaActor is stopped..")
      log.info("MyAkkaActorManager will be stopped")
      context.stop(self)
      log.info("MyAkkaActorManager is stopped")
      context.system.shutdown()
      log.info("system is done")

  }
}
class MyAkkaActor extends Actor {
  def receive: Actor.Receive = {
    case Done => println("MyAkkaActor is done!")
      sender!Done
  }

}

sealed class Commander
case class Done extends Commander
case class Start extends Commander
