package org.ffmmx.example.akka

import java.io.File

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive

object AkkaQuery {

}

case class Document(content:String)
case class Result(keywords:String,line:Int,column:Int)
case class Results(results: Seq[Result]) {
  def size:Int=results.size
}
case class SearchQuery(keyword:String,document: Document)

object Document {
  def fromFile(file:File):Document = ???
}

trait Query {
  def query(keywords:Seq[String]):Results
}

sealed trait Node

trait SearchNode  extends Node with Actor {
  def receive: Actor.Receive = {
    case SearchQuery(keyword,document) =>
      val results = context.actorOf(props = Props[ResultNode])

      sender() ! Results(Seq[Result](Result(keyword,0,0)))
  }
}

trait ResultNode  extends Node with Actor {
  def receive: Actor.Receive = {
    case
  }
}
