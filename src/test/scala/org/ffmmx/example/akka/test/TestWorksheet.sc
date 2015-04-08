object TestWorksheet {

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
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
  println(intBuilder.next)
}