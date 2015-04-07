package org.ffmmx.example.akka

import akka.agent.Agent

object AkkaAgent {
  import scala.concurrent.ExecutionContext.Implicits.global

  def agentTest: Unit = {
    val agent = Agent(5)
    val result = agent()
    val result2 = agent.get

    println("result="+result)
    println("result2="+result2)

//    agent.send(7)
    println("agent.send(7) = "+ agent.send(7))
//    agent.send(_ + 1)
    println("agent.send(_ + 1) = "+agent.send(_ + 1))
    agent.send(_ * 2)
    println("agent.send(_ * 2) = "+agent.send(_ * 2))

    val future=agent.future()
    future.foreach( x => println("future = " + x))


  }

  def monadic:Unit={
    val agent1=Agent(3)
    val agent2=Agent(5)

    for(value <- agent1)
      println("value = "+value)

    val agent3=for(value<-agent1) yield value+1
    val agent4=agent1 map(_+1)
    val agent5=for{
      value1 <- agent1
      value2 <-agent2
    } yield value1 + value2

    println(agent1.get())
    println(agent2.get())
    println(agent3.get())
    println(agent4.get())
    println(agent5.get())
  }
}
