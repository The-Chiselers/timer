//package tech.rocksavage.chiselware.Timer
//
//import java.io.BufferedWriter
//import java.io.File
//import java.io.FileWriter
//import java.io.PrintWriter
//import java.{util => ju}
//
//import scala.math.pow
//import scala.util.Random
//
//import org.scalatest.Assertions._
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//
////import tech.rocksavage.chiselware.util.TestUtils.{randData, checkCoverage}
//import TestUtils.checkCoverage
//import TestUtils.randData
//import chisel3._
//import chisel3.util._
//import chiseltest._
//import chiseltest.coverage._
//import chiseltest.simulator._
//import firrtl2.AnnotationSeq
//import firrtl2.annotations.Annotation // Correct Annotation type for firrtl2
//import firrtl2.options.TargetDirAnnotation
//
///** Highly randomized test suite driven by configuration parameters. Includes
//  * code coverage for all top-level ports. Inspired by the DynamicFifo
//  */
//
//class TimerCounterTest
//    extends AnyFlatSpec
//    with ChiselScalatestTester
//    with Matchers {
//
//  val verbose  = false
//  val numTests = 1
//  val testName = System.getProperty("testName")
//  println(s"Argument passed: $testName")
//
//  // System properties for flags
//  val enableVcd    = System.getProperty("enableVcd", "false").toBoolean
//  val enableFst    = System.getProperty("enableFst", "false").toBoolean
//  val useVerilator = System.getProperty("useVerilator", "false").toBoolean
//
//  val buildRoot = sys.env.get("BUILD_ROOT_RELATIVE")
//  if (buildRoot.isEmpty) {
//    println("BUILD_ROOT_RELATIVE not set, please set and run again")
//    System.exit(1)
//  }
//  val testDir = buildRoot.get + "/test"
//
//  println(
//    s"Test: $testName, VCD: $enableVcd, FST: $enableFst, Verilator: $useVerilator"
//  )
//
//  // Constructing the backend annotations based on the flags
//  val backendAnnotations = {
//    var annos: Seq[Annotation] = Seq() // Initialize with correct type
//
//    if (enableVcd) annos = annos :+ chiseltest.simulator.WriteVcdAnnotation
//    if (enableFst) annos = annos :+ chiseltest.simulator.WriteFstAnnotation
//    if (useVerilator) {
//      annos = annos :+ chiseltest.simulator.VerilatorBackendAnnotation
//      annos = annos :+ VerilatorCFlags(Seq("--std=c++17"))
//    }
//    annos = annos :+ TargetDirAnnotation(testDir)
//
//    annos
//  }
//
//  // Execute the regressigiyon across a randomized range of configurations
//  // if (testName == "regression") (1 to numTests).foreach(config => main(testName))
//  main(testName)
//
//  def main(testName: String): Unit = {
//    behavior of testName
//
//    // Randomize Input Variables
//    // Randomize Input Variables
//    // val validGpioWidths = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//    //   16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)
//    // val validPDataWidths = Seq(8, 16, 32)
//    // val validPAddrWidths = Seq(8, 16, 32)
//    // val PDATA_WIDTH = validPDataWidths(Random.nextInt(validPDataWidths.length))
//    // val PADDR_WIDTH = validPAddrWidths(Random.nextInt(validPAddrWidths.length))
//    // val gpioWidth = {
//    //   val eligibleWidths = validGpioWidths.filter(_ <= PDATA_WIDTH)
//    //   eligibleWidths(Random.nextInt(eligibleWidths.length))
//    // }
//    // Ensure PDATA_WIDTH is equal to dataWidth
//    // assert(
//    //   gpioWidth <= PDATA_WIDTH,
//    //   s"PDATA_WIDTH ($PDATA_WIDTH) should be >= gpioWidth ($gpioWidth)"
//    // )
//
//    // Pass in randomly selected values to DUT
//    // case class BaseParams(
//    //     // wordWidth: Int = 8,
//    //     dataWidth: Int = 16,
//    //     addrWidth: Int = 16,
//    //     mode: Int = 0,
//    //     coverage: Boolean = false,
//    //     // ---
//    //     timer0: Int = 16
//    // )
//    val dataWidth = 16
//    val addrWidth = 16
//    val mode      = 0
//    val timer0    = 16
//    val myParams: BaseParams =
//      BaseParams(
//        dataWidth = 16,
//        addrWidth = 16,
//        mode = 0,
//        coverage = false,
//        timer0 = 16
//      )
//
//    it should "pass" in {
//      info(s"Timer Width = $timer0")
//      //   info(s"APB Data Width = $PDATA_WIDTH")
//      //   info(s"Address Width = $PADDR_WIDTH")
//      info("--------------------------------")
//      val cov = test(new TimerCounter(myParams))
//        .withAnnotations(backendAnnotations) { dut =>
//          dut.clock.setTimeout(0)
//
//          // Reset Sequence
//          resetSequence(dut)
//
//          testName match {
//            case "timerCounterBasic" =>
//              timerCounterBasic(dut)
//            case "allTests" =>
//              allTests(dut)
//          }
//        }
//
//      // Check that all ports have toggled and print report
//      //   if (myParams.coverage) {
//      // val coverage = cov.getAnnotationSeq
//      //   .collectFirst { case a: TestCoverage => a.counts }
//      //   .get
//      //   .toMap
//
//      // val testConfig =
//      //   myParams.gpioWidth.toString + "_" + myParams.PDATA_WIDTH.toString + "_" +
//      //     myParams.PADDR_WIDTH.toString
//
//      // val buildRoot = sys.env.get("BUILD_ROOT")
//      // if (buildRoot.isEmpty) {
//      //   println("BUILD_ROOT not set, please set and run again")
//      //   System.exit(1)
//      // }
//      // // path join
//      // val scalaCoverageDir = new File(buildRoot.get + "/cov/scala")
//      // val verCoverageDir   = new File(buildRoot.get + "/cov/verilog")
//      // verCoverageDir.mkdir()
//      // val coverageFile = verCoverageDir.toString + "/" + testName + "_" +
//      //   testConfig + ".cov"
//
//      // val stuckAtFault = checkCoverage(coverage, coverageFile)
//      // if (stuckAtFault)
//      //   println(
//      //     s"WARNING: At least one IO port did not toggle -- see $coverageFile"
//      //   )
//      // info(s"Verilog Coverage report written to $coverageFile")
//      //   }
//
//    }
//  }
//
////   def allTests(
////       dut: Gpio,
////       gpioDataBuffer: Seq[UInt],
////       apbDataBuffer: Seq[UInt],
////       myParams: BaseParams
////   ): Unit = {
////     basicRegisterRW.basicRegisterRW(
////       dut,
////       gpioDataBuffer,
////       apbDataBuffer,
////       myParams
////     )
////     modeOperation.modeOperation(dut, gpioDataBuffer, apbDataBuffer, myParams)
////     interruptTriggers.interruptTriggers(
////       dut,
////       gpioDataBuffer,
////       apbDataBuffer,
////       myParams
////     )
////     maskingRegisters.maskingRegisters(
////       dut,
////       gpioDataBuffer,
////       apbDataBuffer,
////       myParams
////     )
////     virtualPorts.virtualPorts(dut, gpioDataBuffer, apbDataBuffer, myParams)
////     virtualPorts.virtualPorts(dut, gpioDataBuffer, apbDataBuffer, myParams)
////   }
//
////   class TimerCounter(p: BaseParams) extends Module {
////     val io = IO(new Bundle {
////         val increment       = Input(UInt(p.timer0.W))
////         val set_count_value = Input(UInt(p.timer0.W))
////         val set_count       = Input(Bool())
////         val count           = Output(UInt(p.timer0.W))
////     })
//
////     val count = RegInit(0.U(p.timer0.W))
//
////     count := count + io.increment
//
////     when(io.set_count) {
////         count := io.set_count_value
////     }
//
////     io.count := count
////     }
//
//  def resetSequence(dut: TimerCounter): Unit = {
//    dut.reset.poke(true.B)
//    dut.clock.step()
//    dut.reset.poke(false.B)
//  }
//
//  def timerCounterBasic(dut: TimerCounter): Unit = {
//    val increment       = Random.nextInt(100)
//    val set_count_value = Random.nextInt(100)
//    val set_count       = Random.nextBoolean()
//
//    dut.io.increment.poke(increment.U)
//    dut.io.set_count_value.poke(set_count_value.U)
//    dut.io.set_count.poke(set_count.B)
//
//    dut.clock.step()
//
//    val expectedCount = if (set_count) set_count_value else increment
//    assert(dut.io.count.peek().litValue == expectedCount)
//  }
//  def allTests(
//      dut: TimerCounter
//  ): Unit = {
//    timerCounterBasic(dut)
//    resetSequence(dut)
//    // .. other testst
//  }
//}
