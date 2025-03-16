// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.timer

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import tech.rocksavage.chiselware.apb.ApbTestUtils._
import tech.rocksavage.chiselware.timer.param.TimerParams

object TimerBasicTests extends AnyFlatSpec with ChiselScalatestTester {

    def timerBasicTest(
        dut: Timer,
        myParams: TimerParams): Unit = {
        implicit val clock = dut.clock

        // Get the register map from the Timer module
        val registerMap = dut.registerMap

        // Get the addresses of the registers
        val enAddr = registerMap.getAddressOfRegister("en").get
        val prescalerAddr =
            registerMap.getAddressOfRegister("prescaler").get
        val maxCountAddr =
            registerMap.getAddressOfRegister("maxCount").get
        val pwmCeilingAddr =
            registerMap.getAddressOfRegister("pwmCeiling").get
        val setCountValueAddr =
            registerMap.getAddressOfRegister("setCountValue").get
        registerMap.getAddressOfRegister("setCount").get

        // Write to the prescaler register
        writeAPB(dut.io.apb, prescalerAddr.U, 0.U)
        readAPB(dut.io.apb, prescalerAddr.U) shouldEqual 0

        // Write to the maxCount register
        writeAPB(dut.io.apb, maxCountAddr.U, 32.U)
        readAPB(dut.io.apb, maxCountAddr.U) shouldEqual 32

        // Write to the pwmCeiling register
        writeAPB(dut.io.apb, pwmCeilingAddr.U, 5.U)
        readAPB(dut.io.apb, pwmCeilingAddr.U) shouldEqual 5

        // Write to the setCountValue register
        writeAPB(dut.io.apb, setCountValueAddr.U, 0.U)
        readAPB(dut.io.apb, setCountValueAddr.U) shouldEqual 0

        // Write to the setCount register
//        writeAPB(dut.io.apb, setCountAddr.U, 1.U)
//        readAPB(dut.io.apb, setCountAddr.U) shouldEqual 1

        // Write to the enable register
        writeAPB(dut.io.apb, enAddr.U, 1.U)
        readAPB(dut.io.apb, enAddr.U) shouldEqual 1

        // Step the clock until the count reaches maxCount
        dut.clock.setTimeout(0)
        while (dut.io.timerOutput.count.peekInt() < 30)
            dut.clock.step(1)

        // Check that maxReached is true
        dut.io.timerOutput.pwm.expect(
          true.B,
        ) // Since count >= pwmCeiling, PWM should be high
    }
    // Test 1: Set PWM ceiling and sample at every clock cycle to verify its correct for one cycle
    def testPWMCeiling(
        dut: Timer,
        myParams: TimerParams): Unit = {
        implicit val clock = dut.clock
        // Get the register map and addresses
        val registerMap = dut.registerMap

        val enAddr         = registerMap.getAddressOfRegister("en").get
        val prescalerAddr  = registerMap.getAddressOfRegister("prescaler").get
        val maxCountAddr   = registerMap.getAddressOfRegister("maxCount").get
        val pwmCeilingAddr = registerMap.getAddressOfRegister("pwmCeiling").get
        registerMap.getAddressOfRegister("setCountValue").get
        registerMap.getAddressOfRegister("setCount").get

        // Configure the timer
        // Set prescaler to 0 (no prescaling)
        writeAPB(dut.io.apb, prescalerAddr.U, 0.U)
        // Set maxCount to 10
        writeAPB(dut.io.apb, maxCountAddr.U, 10.U)
        // Set pwmCeiling to 5
        writeAPB(dut.io.apb, pwmCeilingAddr.U, 5.U)
        // Enable the timer last
        writeAPB(dut.io.apb, enAddr.U, 1.U)

        var prevCount = 0

        // Sample at every clock cycle for one period
        val maxCountValue = 10
        for (i <- 0 until maxCountValue) {
            dut.clock.step(1)
            val count       = dut.io.timerOutput.count.peekInt().toInt
            val pwm         = dut.io.timerOutput.pwm.peekBoolean()
            val expectedPwm = prevCount >= 5
            prevCount = count
            assert(
              pwm == expectedPwm,
              s"At count $count, expected PWM $expectedPwm but got $pwm",
            )
        }
    }

    // Test 2: Change the prescaler halfway through execution and check timing
    def testPrescalerChange(
        dut: Timer,
        myParams: TimerParams): Unit = {
        implicit val clock = dut.clock
        val registerMap    = dut.registerMap

        val enAddr        = registerMap.getAddressOfRegister("en").get
        val prescalerAddr = registerMap.getAddressOfRegister("prescaler").get
        val maxCountAddr  = registerMap.getAddressOfRegister("maxCount").get

        // Set initial prescaler to 1
        writeAPB(dut.io.apb, prescalerAddr.U, 1.U)
        // Set maxCount to 20
        writeAPB(dut.io.apb, maxCountAddr.U, 20.U)
        // Enable the timer last
        writeAPB(dut.io.apb, enAddr.U, 1.U)

        // 2 clock cycles for apb write to finish
        var totalCycles    = 0
        
        var prescalerValue = 1

        
        

        val cyclesToCount10     = 19 // has to account for time for apb write
        val cyclesFromCount10   = 34 // has to account for time for apb write
        val expectedTotalCycles = cyclesToCount10 + cyclesFromCount10
        while (dut.io.timerOutput.maxReached.peek().litToBoolean == false) {
            if (dut.io.timerOutput.count.peekInt() == 10) {
                assert(
                  totalCycles == cyclesToCount10, // 0 - 10 is 11 counts
                  s"Total cycles $totalCycles != expected $cyclesToCount10",
                )
                // Change prescaler to 3
                writeAPB(dut.io.apb, prescalerAddr.U, 3.U)
                prescalerValue = 3
            }
            dut.clock.step(1)
            totalCycles += 1
        }

        assert(
          totalCycles == expectedTotalCycles,
          s"Total cycles $totalCycles != expected $expectedTotalCycles",
        )
    }

    // Test 3: Set maxCount to a low value and verify duty cycle over 10 periods
    def testLowMaxCountDutyCycle(
        dut: Timer,
        myParams: TimerParams): Unit = {
        implicit val clock = dut.clock
        val registerMap    = dut.registerMap

        val enAddr         = registerMap.getAddressOfRegister("en").get
        val prescalerAddr  = registerMap.getAddressOfRegister("prescaler").get
        val maxCountAddr   = registerMap.getAddressOfRegister("maxCount").get
        val pwmCeilingAddr = registerMap.getAddressOfRegister("pwmCeiling").get

        // Set prescaler to 0 (no prescaling)
        writeAPB(dut.io.apb, prescalerAddr.U, 0.U)
        // Set maxCount to 4
        writeAPB(dut.io.apb, maxCountAddr.U, 4.U)
        // Set pwmCeiling to 2
        writeAPB(dut.io.apb, pwmCeilingAddr.U, 2.U)
        // Enable the timer last
        writeAPB(dut.io.apb, enAddr.U, 1.U)

        val totalPeriods  = 10
        val maxCountValue = 4
        val totalCycles   = totalPeriods * (maxCountValue + 1)
        var pwmHighCount  = 0
        var pwmLowCount   = 0

        for (_ <- 0 until totalCycles) {
            dut.clock.step(1)
            val pwm = dut.io.timerOutput.pwm.peekBoolean()
            if (pwm) pwmHighCount += 1 else pwmLowCount += 1
        }

        val expectedDutyCycle = (maxCountValue - 2).toDouble / maxCountValue
        val actualDutyCycle =
            pwmHighCount.toDouble / (pwmHighCount + pwmLowCount)

        assert(
          math.abs(actualDutyCycle - expectedDutyCycle) < 0.1,
          s"Expected duty cycle $expectedDutyCycle, but got $actualDutyCycle",
        )
    }

    // Test 4: Random maxCount and prescaler, verify total cycles until maxReached
    def testRandomMaxCountAndPrescaler(
        dut: Timer,
        myParams: TimerParams): Unit = {
        implicit val clock = dut.clock
        val registerMap    = dut.registerMap

        val enAddr        = registerMap.getAddressOfRegister("en").get
        val prescalerAddr = registerMap.getAddressOfRegister("prescaler").get
        val maxCountAddr  = registerMap.getAddressOfRegister("maxCount").get

        val rand    = new scala.util.Random
        val unix_ms = System.currentTimeMillis()
        rand.setSeed(unix_ms)
        val maxCountValue  = rand.nextInt(50) + 1 // Ensure non-zero
        val prescalerValue = rand.nextInt(5)

        // Set prescaler
        writeAPB(dut.io.apb, prescalerAddr.U, prescalerValue.U)
        // Set maxCount
        writeAPB(dut.io.apb, maxCountAddr.U, maxCountValue.U)
        // Enable the timer last
        writeAPB(dut.io.apb, enAddr.U, 1.U)

        // Calculate expected total cycles
        val expectedTotalCycles =
            maxCountValue * (prescalerValue + 1) - 1 // -1 for apb write
        var cycles = 0

        while (
          !dut.io.timerOutput.maxReached
              .peekBoolean() && cycles < expectedTotalCycles * 2
        ) {
            dut.clock.step(1)
            cycles += 1
        }

        println(
          s"Random Test: maxCount=$maxCountValue, prescaler=$prescalerValue, totalCycles=$cycles",
        )

        assert(
          cycles == expectedTotalCycles,
          s"Total cycles $cycles != expected $expectedTotalCycles",
        )
    }
}
