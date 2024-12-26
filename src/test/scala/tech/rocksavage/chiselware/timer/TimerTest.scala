package tech.rocksavage.chiselware.timer

import chiseltest.ChiselScalatestTester
import chiseltest.formal.{BoundedCheck, Formal}
import org.scalatest.flatspec.AnyFlatSpec

class TimerTest
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Formal {

  "Timer" should "formally verify" in {

    val addrWidth: Int  = 32
    val dataWidth: Int  = 32

    val countWidth: Int = 16

    val p = TimerParams(dataWidth, addrWidth, countWidth)

    verify(new TimerInner(p, true), Seq(BoundedCheck(100)))

  }
}
