package org.ffmmx.example.akka

import java.io.File

import akka.actor.Actor.Receive
import akka.actor._
import akka.pattern._
import akka.util.Timeout

import scala.collection.immutable.HashMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

object AkkaQuery {

}

case class Document(content:String) {
  def length:Int = content.length
  def subDocument(start:Int,length:Int):Document = ???
  def subDocument(start:Int):Document = ???
}
object Document {
  def fromFile(file:File):Document = ???
}
case class Result(keywords:String,line:Int,column:Int,offset:Int)
case class Results(results: Seq[Result]) {
  def size:Int=results.size
}
case class SearchQuery(keyword:String,max:Int,response: ActorRef)
case class Response(results:Seq[(Double,String)])

class SearchNode(id:Int) extends Actor {
  def receive: Receive = {
    case SearchQuery(query,maxDocs,handler) =>
      val result = for {
        results <- index.get(query).toList
        resultList <- results
      } yield resultList
      handler ! Response(result)
  }

  lazy val index:HashMap[String,Seq[(Double,String)]] = {
    def makeIndex(docs:String*)={
      var tmp=HashMap[String,Seq[(Double,String)]]()
      for(doc <- docs;(key,value) <- doc.split("\\s+").groupBy(identity)) {
        val list=tmp.get(key).getOrElse(Seq())
        tmp+=((key,((value.length.toDouble,doc)) +: list))
      }
      tmp
    }
    id match  {
      case 1 => makeIndex("Some example data for you")
      case 2 => makeIndex("Some more example data for you to use")
      case 3 => makeIndex("To be or not to be, that is the question")
      case 4 => makeIndex("OMG it's a cat")
      case 5 => makeIndex("This is an example.  It's a great one")
      case 6 => makeIndex("HAI there", "HAI IZ HUNGRY")
      case 7 => makeIndex("Hello, World")
      case 8 => makeIndex("Hello, and welcome to the search node 8")
      case 9 => makeIndex("The lazy brown fox jumped over the")
      case 10 => makeIndex("Winning is the best because it's winning.")
    }
  }
}

class HeadNode extends Actor {
  def nodes:Seq[ActorRef] = Seq()

  def receive: Actor.Receive = {
    case SearchQuery(q,max,responder) =>
      val gatherer = context.actorOf(Props(new GathererNode {
        val client: ActorRef = responder
        val maxDocs: Int = max
        val maxResponses: Int = nodes.size
        val query: String = q
      }))
      for(node <- nodes){
        node!SearchQuery(q,max,gatherer)
      }
  }
}

trait GathererNode extends Actor {
  val maxDocs :Int
  val query:String
  val maxResponses:Int
  val client:ActorRef

  var results=Seq[(Double,String)]()
  var responseCount=0
  context.setReceiveTimeout(Timeout(5 seconds).duration)
  private  def combineResults(current : Seq[(Double,String)],next:Seq[(Double,String)])=
    (current ++ next).view.sortBy(_._1).take(maxDocs).force

  def receive: Actor.Receive = {
    case Response(next) =>
      results=combineResults(results,next)
      responseCount+=1
      if(responseCount==maxResponses){
        client!Response(results)
        context.stop(self)
      }
    case ReceiveTimeout=>
      client!Response(Seq())
      context.stop(self)
  }
}


case class SearchableDocument(content:String)

trait AdapativeSearchNode extends Actor with BaseHeadNode with BaseChildNode{
  def receive: Actor.Receive = leafNode

  protected def split(): Unit = {
    children = (for(docs<-documents.grouped(5)) yield {
      val child=context.actorOf(Props[AdapativeSearchNode])
      docs.foreach(child ! SearchableDocument(_))
      child
    }).toIndexedSeq
    clearIndex()
    this become parentNode
  }
}
trait BaseHeadNode {
  self : AdapativeSearchNode =>
  var children=IndexedSeq[ActorRef]()
  var currentIdex=0
  def parentNode:PartialFunction[Any,Unit]={
    case SearchQuery(q,max,responder) =>
      val gatherer= context.actorOf(Props(new GathererNode {val client: ActorRef = responder
        val maxDocs: Int = max
        val maxResponses: Int = children.size
        val query: String = q
      }))

      for(node <- children){
        node ! SearchQuery(q,max,gatherer)
      }

    case s @ SearchableDocument(_) => getNextChild ! s
  }
  private def getNextChild = {
    currentIdex=(1+currentIdex) % children.size
    children(currentIdex)
  }
}

trait BaseChildNode{
  self:AdapativeSearchNode=>
  final val maxNoOfDocuments=10
  var documents:Vector[String]=Vector()
  var index:HashMap[String,Seq[(Double,String)]] = HashMap()

  def leafNode:PartialFunction[Any,Unit]={
    case SearchQuery(query,maxDocs,handler)=>
      executeLocalQuery(query,maxDocs,handler)
    case SearchableDocument(content) =>
      addDocumentToLocalIndex(content)
  }

  private def executeLocalQuery(query:String,maxDocs:Int,handler:ActorRef)={
    val result= for {
      results <- index.get(query).toList
      resultList<- results
    } yield resultList
    handler!Response(result take maxDocs)
  }

  private def addDocumentToLocalIndex(content:String)={
    for ( (key,value) <- content.split("\\s+").groupBy(identity)) {
      val list=index.get(key).getOrElse(Seq())
      index += ((key,((value.length.toDouble,content)) +: list))
    }
    documents=documents:+content
    if(documents.size > maxNoOfDocuments) split()
  }

  protected def split():Unit
  protected def clearIndex():Unit={
    documents=Vector()
    index=HashMap()
  }
}

