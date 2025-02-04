package tech.rocksavage.chiselware.timer

import chiseltest.RawTester.verify
import chiseltest._
import chiseltest.formal.BoundedCheck
import chiseltest.simulator._
import firrtl2.annotations.Annotation
import firrtl2.options.TargetDirAnnotation
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.test.coverageCollector

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
    val useVerilator = System.getProperty("useVerilator", "false").toBoolean

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
            annos = annos :+ VerilatorCFlags(Seq("--std=c++17 -O3"))
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

    def main(testNameMaybeNull: String): Unit = {
        val testName =
            if (testNameMaybeNull == null) "" else testNameMaybeNull
        behavior of testName

        val covDir = "./out/cov"
        val coverage = true

        // Randomize Input Variables
        val dataWidth      = 32
        val addrWidth      = 32
        val countWidth     = 32
        val prescalerWidth = 32


        // Pass in randomly selected values to DUT
        val timerParams = TimerParams(32, 32, 32, 32, verbose = true)
        val configName = "32_32_32_32"

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
            case "pwm_ceiling_test" =>
                it should "verify PWM ceiling functionality" in {
                    val cov = test(new Timer(timerParams, false))
                        .withAnnotations(backendAnnotations) { dut =>
                            TimerBasicTests.testPWMCeiling(dut, timerParams)
                        }
                    coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
                }
            case "prescaler_change_test" =>
                it should "verify prescaler change during execution" in {
                    val cov = test(new Timer(timerParams, false))
                        .withAnnotations(backendAnnotations) { dut =>
                            TimerBasicTests.testPrescalerChange(
                              dut,
                              timerParams
                            )
                        }
                    coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
                }
            case "low_maxcount_dutycycle_test" =>
                it should "verify duty cycle with low maxCount over multiple cycles" in {
                    val cov = test(new Timer(timerParams, false))
                        .withAnnotations(backendAnnotations) { dut =>
                            TimerBasicTests.testLowMaxCountDutyCycle(
                              dut,
                              timerParams
                            )
                        }
                    coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
                }
            case "random_test" =>
                it should "verify timer with random maxCount and prescaler" in {
                    val cov = test(new Timer(timerParams, false))
                        .withAnnotations(backendAnnotations) { dut =>
                            TimerBasicTests.testRandomMaxCountAndPrescaler(
                              dut,
                              timerParams
                            )
                        }
                }
            case "basic" =>
                it should "pass a basic test" in {
                    val cov = test(
                      new Timer(
                        timerParams,
                        false
                      )
                    )
                        .withAnnotations(backendAnnotations) { dut =>
                            TimerBasicTests.timerBasicTest(
                              dut,
                                timerParams,
                            )
                        }
                    coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
                }
            case _ => allTests(timerParams, configName, covDir, coverage)
        }
        it should "generate cumulative coverage report" in {
            coverageCollector.saveCumulativeCoverage(coverage, covDir)
        }
    }

    def allTests(timerParams: TimerParams, configName: String, covDir: String, coverage: Boolean): Unit = {
        "TimerInner" should "Formally Verify" in {
            verify(
              new TimerInnerFVHarness(timerParams, true),
              Seq(BoundedCheck(40))
            )
        }

        it should "verify PWM ceiling functionality" in {
            val cov = test(new Timer(timerParams, false))
                .withAnnotations(backendAnnotations) { dut =>
                    TimerBasicTests.testPWMCeiling(dut, timerParams)
                }
            coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
        }

        it should "verify prescaler change during execution" in {
            val cov = test(new Timer(timerParams, false))
                .withAnnotations(backendAnnotations) { dut =>
                    TimerBasicTests.testPrescalerChange(dut, timerParams)
                }
            coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
        }
        it should "verify duty cycle with low maxCount over multiple cycles" in {
            val cov = test(new Timer(timerParams, false))
                .withAnnotations(backendAnnotations) { dut =>
                    TimerBasicTests.testLowMaxCountDutyCycle(dut, timerParams)
                }
            coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
        }
        it should "verify timer with random maxCount and prescaler" in {
            val cov = test(new Timer(timerParams, false))
                .withAnnotations(backendAnnotations) { dut =>
                    TimerBasicTests.testRandomMaxCountAndPrescaler(
                      dut,
                      timerParams
                    )
                }
            coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
        }
        it should "pass a basic test" in {
            val timerParams = TimerParams(32, 32, 32, 32, verbose = true)
            val cov = test(new Timer(timerParams, false))
                .withAnnotations(backendAnnotations) { dut =>
                    TimerBasicTests.timerBasicTest(
                      dut,
                        timerParams
                    )
                }
            coverageCollector.collectCoverage(cov.getAnnotationSeq, testName, configName, coverage, covDir)
        }
    }

}
