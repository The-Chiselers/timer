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
        myParams: TimerParams
    ): Unit = {
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
        val setCountAddr =
            registerMap.getAddressOfRegister("setCount").get

        // Write to the prescaler register
        writeAPB(dut.io.apb, prescalerAddr.U, 10.U)
        readAPB(dut.io.apb, prescalerAddr.U) shouldEqual 10

        // Write to the maxCount register
        writeAPB(dut.io.apb, maxCountAddr.U, 1024.U)
        readAPB(dut.io.apb, maxCountAddr.U) shouldEqual 1024

        // Write to the pwmCeiling register
        writeAPB(dut.io.apb, pwmCeilingAddr.U, 50.U)
        readAPB(dut.io.apb, pwmCeilingAddr.U) shouldEqual 50

        // Write to the setCountValue register
        writeAPB(dut.io.apb, setCountValueAddr.U, 20.U)
        readAPB(dut.io.apb, setCountValueAddr.U) shouldEqual 20

        // Write to the setCount register
        writeAPB(dut.io.apb, setCountAddr.U, 1.U)
        readAPB(dut.io.apb, setCountAddr.U) shouldEqual 1

        // Write to the enable register
        writeAPB(dut.io.apb, enAddr.U, 1.U)
        readAPB(dut.io.apb, enAddr.U) shouldEqual 1

        // Step the clock until the count reaches maxCount
        while (dut.io.timerOutput.count.peekInt() < 1000) {
            dut.clock.step(1)
        }

        // Check that maxReached is true
        dut.io.timerOutput.pwm.expect(
          true.B
        ) // Since count >= pwmCeiling, PWM should be high
    }
}
