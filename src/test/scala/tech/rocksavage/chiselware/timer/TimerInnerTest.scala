//package tech.rocksavage.chiselware.timer
//
//import chiseltest.ChiselScalatestTester
//import chiseltest.formal.{BoundedCheck, Formal}
//import org.scalatest.flatspec.AnyFlatSpec
//import tech.rocksavage.chiselware.timer.param.TimerParams
//
//class TimerInnerTest
//    extends AnyFlatSpec
//    with ChiselScalatestTester
//    with Formal {
//
//    "TimerInner" should "formally verify" in {
//
//        val addrWidth: Int = 32
//        val dataWidth: Int = 32
//
//        val countWidth: Int     = 16
//        val prescalerWidth: Int = 16
//
//        val p = TimerParams(dataWidth, addrWidth, countWidth, prescalerWidth)
//
//        verify(new TimerInnerFVHarness(p, true), Seq(BoundedCheck(100)))
//
//    }
//}
