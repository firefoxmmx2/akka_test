package org.ffmmx.example.akka

import java.io.File

import akka.actor.{Actor, Props}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

object AkkaQuery {

}

case class Document(content:String) {
  def length:Int = content.length
  def subDocument(start:Int,length:Int):Document = ???
  def subDocument(start:Int):Document = ???
}
case class Result(keywords:String,line:Int,column:Int,offset:Int)
case class Results(results: Seq[Result]) {
  def size:Int=results.size
}
case class SearchQuery(keyword:String,document: Document)
case class ResultQ(result: Result,document: Document)
object Document {
  def fromFile(file:File):Document = ???
}

sealed trait Node

trait SearchQueryNode  extends Node with Actor {
  implicit val timeout=Timeout(5 seconds)
  def receive: Actor.Receive = {
    case SearchQuery(keyword,document) =>
      val resultNode = context.actorOf(props = Props[ResultNode])
      val result1=resultNode ? SearchQuery(keyword,document.subDocument(0,document.length/2))
      val result2=resultNode ? SearchQuery(keyword,document.subDocument(document.length/2))
      val result3 = Await.result(Future.sequence(Seq(result1.mapTo[Result],  result2.mapTo[Result])),timeout.duration)

    case ResultQ(result,document) =>
      if(result!=null && document != null && document.length / 2 > result.keywords.length){
        self ! SearchQuery(result.keywords,document)
      }
    case Results(results) =>
  }
}

trait ResultNode  extends Node with Actor {
  def query(keyword:String,document: Document):Result = ???
  def receive: Actor.Receive = {
    case SearchQuery(keyword,document) =>
      sender() ! ResultQ(query(keyword, document),document)
  }
}
