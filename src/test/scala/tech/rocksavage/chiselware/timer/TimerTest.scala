// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.timer

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import tech.rocksavage.chiselware.apb.ApbBundle
import tech.rocksavage.chiselware.apb.ApbTestUtils._
import tech.rocksavage.chiselware.addressable.RegisterMap
import tech.rocksavage.chiselware.timer.Timer
import tech.rocksavage.chiselware.timer.bundle.TimerInterruptEnum

class TimerTest extends AnyFlatSpec with ChiselScalatestTester {

  "Timer" should "correctly handle register writes and reads" in {
    test(new Timer()) { dut =>
      implicit val clock = dut.clock

      // Get the register map from the Timer module
      val registerMap = dut.registerMap

      // Get the addresses of the registers
      val enAddr = registerMap.getAddressOfRegister("en").get
      val prescalerAddr = registerMap.getAddressOfRegister("prescaler").get
      val maxCountAddr = registerMap.getAddressOfRegister("maxCount").get
      val pwmCeilingAddr = registerMap.getAddressOfRegister("pwmCeiling").get
      val setCountValueAddr = registerMap.getAddressOfRegister("setCountValue").get
      val setCountAddr = registerMap.getAddressOfRegister("setCount").get

      // Write to the enable register
      writeAPB(dut.io.apb, enAddr.U, 1.U)
      readAPB(dut.io.apb, enAddr.U) shouldEqual 1

      // Write to the prescaler register
      writeAPB(dut.io.apb, prescalerAddr.U, 10.U)
      readAPB(dut.io.apb, prescalerAddr.U) shouldEqual 10

      // Write to the maxCount register
      writeAPB(dut.io.apb, maxCountAddr.U, 100.U)
      readAPB(dut.io.apb, maxCountAddr.U) shouldEqual 100

      // Write to the pwmCeiling register
      writeAPB(dut.io.apb, pwmCeilingAddr.U, 50.U)
      readAPB(dut.io.apb, pwmCeilingAddr.U) shouldEqual 50

      // Write to the setCountValue register
      writeAPB(dut.io.apb, setCountValueAddr.U, 20.U)
      readAPB(dut.io.apb, setCountValueAddr.U) shouldEqual 20

      // Write to the setCount register
      writeAPB(dut.io.apb, setCountAddr.U, 1.U)
      readAPB(dut.io.apb, setCountAddr.U) shouldEqual 1

      // Step the clock to allow the Timer to process the inputs
      dut.clock.step(10)

      // Check the timer output
      dut.io.timerOutput.count.expect(20.U) // Since setCount was enabled, the count should be set to setCountValue
      dut.io.timerOutput.maxReached.expect(false.B)
      dut.io.timerOutput.pwm.expect(false.B)

      // Step the clock to allow the Timer to count up
      dut.clock.step(10)

      // Check the timer output again
      dut.io.timerOutput.count.expect(30.U) // count = 20 + 10 (prescaler)
      dut.io.timerOutput.maxReached.expect(false.B)
      dut.io.timerOutput.pwm.expect(false.B)

      // Step the clock until the count reaches maxCount
      while (dut.io.timerOutput.count.peekInt() < 100) {
        dut.clock.step(1)
      }

      // Check that maxReached is true
      dut.io.timerOutput.maxReached.expect(true.B)
      dut.io.timerOutput.pwm.expect(true.B) // Since count >= pwmCeiling, PWM should be high

      // Check the interrupt output
      dut.io.interrupt.interrupt.expect(TimerInterruptEnum.MaxReached)
    }
  }
}