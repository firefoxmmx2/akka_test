package test.resources.scala.org.ffmmx.example.akka.test

import org.specs2.mutable.Specification


/**
 * Created by hooxin on 15-3-25.
 */
object AkkaSpec extends Specification{
  "Akka Actor Sum " should {
    "fork task" in {
      1 must be_==(1)
    }
  }

}
