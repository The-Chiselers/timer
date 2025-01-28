package tech.rocksavage.chiselware.SPI

import chiseltest.RawTester.verify
import chiseltest.formal.BoundedCheck
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.chiselware.timer.{Timer, TimerInnerFVHarness}

import java.io.File
import scala.util.Random

//import tech.rocksavage.chiselware.util.TestUtils.{randData, checkCoverage}
import chiseltest._
import chiseltest.coverage._
import chiseltest.simulator._
import firrtl2.annotations.Annotation
import firrtl2.options.TargetDirAnnotation
import tech.rocksavage.chiselware.SPI.TestUtils.checkCoverage

/** Highly randomized test suite driven by configuration parameters. Includes
  * code coverage for all top-level ports. Inspired by the DynamicFifo
  */

class TimerTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

    val verbose  = false
    val numTests = 2
    val testName = System.getProperty("testName")
    println(s"Argument passed: $testName")

    // System properties for flags
    val enableVcd    = System.getProperty("enableVcd", "true").toBoolean
    val enableFst    = System.getProperty("enableFst", "false").toBoolean
    val useVerilator = System.getProperty("useVerilator", "true").toBoolean

    val testDir = "out/test"

    println(
      s"Test: $testName, VCD: $enableVcd, FST: $enableFst, Verilator: $useVerilator"
    )

    // Constructing the backend annotations based on the flags
    val backendAnnotations = {
        var annos: Seq[Annotation] = Seq() // Initialize with correct type

        if (enableVcd) annos = annos :+ chiseltest.simulator.WriteVcdAnnotation
        if (enableFst) annos = annos :+ chiseltest.simulator.WriteFstAnnotation
        if (useVerilator) {
            annos = annos :+ chiseltest.simulator.VerilatorBackendAnnotation
            annos = annos :+ VerilatorCFlags(Seq("--std=c++17"))
        }
        annos = annos :+ TargetDirAnnotation(testDir)

        annos
    }

    // Execute the regressigiyon across a randomized range of configurations
    if (testName == "regression") (1 to numTests).foreach { config =>
        main(s"Timer_test_config_$config")
    }
    else {
        main(testName)
    }

    def main(testName: String): Unit = {
        behavior of testName

        // Randomize Input Variables
        val validDataWidths      = Seq(8, 16, 32)
        val validPAddrWidths     = Seq(8, 16, 32)
        val validCountWidths     = Seq(8, 16, 32)
        val validPrescalerWidths = Seq(8, 16, 32)

        val dataWidth =
            validDataWidths(Random.nextInt(validDataWidths.length))
        val addrWidth = validPAddrWidths(
          Random.nextInt(validPAddrWidths.length)
        )
        val countWidth = validCountWidths(
          Random.nextInt(validCountWidths.length)
        )
        val prescalerWidth = validPrescalerWidths(
          Random.nextInt(validPrescalerWidths.length)
        )

        // Pass in randomly selected values to DUT
        val timerParams = TimerParams(dataWidth, addrWidth, 8)

        info(s"Data Width = $dataWidth")
        info(s"Address Width = $addrWidth")
        info(s"Count Width = $countWidth")
        info(s"Prescaler Width = $prescalerWidth")
        info("--------------------------------")

        testName match {
            // Test case for Master Mode Initialization
            case "formal" =>
                "TimerInner" should "Formally Verify" in {
                    verify(
                      new TimerInnerFVHarness(timerParams, true),
                      Seq(BoundedCheck(40))
                    )
                }

            // Test case for Slave Mode Initialization
            case "basic" =>
                it should "pass a basic test" in {
                    val cov = test(new Timer(timerParams, false))
                        .withAnnotations(backendAnnotations) { dut =>
                            transmitTests.slaveMode(dut, timerParams)
                        }
                    coverageCollection(
                      cov.getAnnotationSeq,
                      timerParams,
                      testName
                    )
                }

            case "allTests" =>
                allTests(timerParams)

            case _ => allTests(timerParams)
        }

        // Test 6.1: Master Deactivation upon SS Low
        // In a multi-master scenario, configure the SS pin to control master activation.
        // Drive SS low and ensure the SPI automatically switches from Master to Slave mode.

        // Test 6.2: Tri-state MISO in Slave Mode
        // In Slave mode, configure the MISO pin as output.
        // When SS is high, ensure MISO is tri-stated (disconnected).
        // When SS is low, verify that MISO outputs data correctly.

        // Test 7.3: Normal Mode Slave
        // In Slave mode, ensure the SPI logic halts when SS is high and resumes when SS is low.

        // Test 7.4: Buffered Mode Slave
        // Enable Buffered Mode in Slave mode and verify that multiple received bytes are stored in the FIFO and transmitted correctly.

        it should "generate cumulative coverage report" in {
            coverageCollector.saveCumulativeCoverage(timerParams)
        }
    }

    // }

    def allTests(
        timerParams: TimerParams
    ): Unit = {
        transmitTestsFull(timerParams)
        clockTestsFull(timerParams)
        interruptTestsFull(timerParams)
        modeTestsFull(timerParams)
    }

    def transmitTestsFull(
        timerParams: TimerParams
    ): Unit = {

        it should "initialize the SPI core in Master Mode correctly" in {
            val cov =
                test(new SPI(timerParams)).withAnnotations(backendAnnotations) {
                    dut =>
                        transmitTests.masterMode(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "masterMode")
        }

        it should "initialize the SPI core in Slave Mode correctly" in {
            val cov =
                test(new SPI(timerParams)).withAnnotations(backendAnnotations) {
                    dut =>
                        transmitTests.slaveMode(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "slaveMode")
        }

        it should "transmit and receive data correctly in Full Duplex mode (Master-Slave) for all SPI modes" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    transmitTests.fullDuplex(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "fullDuplex")
        }

        it should "transmit and receive data correctly in MSB and LSB first modes" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    transmitTests.bitOrder(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "bitOrder")
        }

    }

    def clockTestsFull(
        timerParams: TimerParams
    ): Unit = {

        it should "clock speed test for prescalar 0x2(64 times slower)" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    clockTests.prescaler(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "prescaler")
        }

        it should "clock speed for clk2x with prescalar of 8 times slower" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    clockTests.doubleSpeed(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "doubleSpeed")
        }
    }

    def interruptTestsFull(
        timerParams: TimerParams
    ): Unit = {

        it should "transmission complete interrupt flag" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    interruptTests.txComplete(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "txComplete")
        }

        it should "write collision flag" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    interruptTests.wcolFlag(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "wcolFLag")
        }

        it should "data register empty interrupt flag" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    interruptTests.dataEmpty(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "dataEmpty")
        }

        it should "cause buffer overflow flag" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    interruptTests.overFlow(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "overFlow")
        }
    }

    def modeTestsFull(
        timerParams: TimerParams
    ): Unit = {
        it should "buffered mode master" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    modeTests.bufferTx(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "bufferTx")
        }

        it should "recieve register correct normal mode" in {
            val cov = test(new FullDuplexSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    modeTests.normalRx(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "normalRx")
        }

        it should "daisy chain correctly" in {
            val cov = test(new DaisyChainSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    modeTests.daisyChain(dut, timerParams)
                }
            coverageCollection(cov.getAnnotationSeq, timerParams, "daisyChain")
        }

        it should "daisy chain + buffer correctly" in {
            val cov = test(new DaisyChainSPI(timerParams))
                .withAnnotations(backendAnnotations) { dut =>
                    modeTests.daisyChainBuffer(dut, timerParams)
                }
            coverageCollection(
              cov.getAnnotationSeq,
              timerParams,
              "daisyChainBuffer"
            )
        }
    }

    def coverageCollection(
        cov: Seq[Annotation],
        timerParams: TimerParams,
        testName: String
    ): Unit = {
        if (timerParams.coverage) {
            val coverage = cov
                .collectFirst { case a: TestCoverage => a.counts }
                .get
                .toMap

            val testConfig =
                timerParams.addressWidth.toString + "_" + timerParams.dataWidth.toString

            val buildRoot = sys.env.get("BUILD_ROOT")
            if (buildRoot.isEmpty) {
                println("BUILD_ROOT not set, please set and run again")
                System.exit(1)
            }
            // path join
            val scalaCoverageDir = new File(buildRoot.get + "/cov/scala")
            val verCoverageDir   = new File(buildRoot.get + "/cov/verilog")
            verCoverageDir.mkdirs()
            val coverageFile = verCoverageDir.toString + "/" + testName + "_" +
                testConfig + ".cov"

            val stuckAtFault = checkCoverage(coverage, coverageFile)
            if (stuckAtFault)
                println(
                  s"WARNING: At least one IO port did not toggle -- see $coverageFile"
                )
            info(s"Verilog Coverage report written to $coverageFile")
        }
    }
}
