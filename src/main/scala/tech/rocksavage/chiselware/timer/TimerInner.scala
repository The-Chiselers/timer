// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chiseltest.formal.past


/** An address decoder that can be used to decode addresses into a set of ranges
  *
  * @constructor
  *   Create a new address decoder
  * @param params
  *   GpioParams object including dataWidth and addressWidth
  * @param formal
  *   A boolean value to enable formal verification
  * @author
  *   Warren Savage
  */
class TimerInner(
  params: TimerParams,
  formal: Boolean = false
) extends Module {


  /** Returns the number of memory addresses used by the module
    *
    * @return
    *   The width of the memory
    */
  val io = IO(new TimerBundle(params))

  // ###################
  // Syncronizers for input / formal verify
  // ###################
  val enReg = RegInit(false.B)
  val prescalerReg = RegInit(0.U(params.countWidth.W))
  val maxCountReg = RegInit(0.U(params.countWidth.W))
  val pwmCeilingReg = RegInit(0.U(params.countWidth.W))

  val setClockValueReg = RegInit(0.U(params.countWidth.W))
  val setClockReg = RegInit(false.B)

  // ###################
  // Registers that hold the output values
  // ###################
  val countReg = RegInit(0.U(params.countWidth.W))
  val maxReachedReg = RegInit(false.B)
  val pwmReg = RegInit(false.B)

  // ###################
  // Next state logic
  // ###################
  val nextCount = WireInit(0.U(params.countWidth.W))
  val nextMaxReached = WireInit(false.B)
  val nextPwm = WireInit(false.B)

  // ###################
  // Instatiation
  // ###################

  enReg := io.en
  prescalerReg := io.prescaler
  maxCountReg := io.maxCount
  pwmCeilingReg := io.pwmCeiling
  setClockValueReg := io.setClockValue
  setClockReg := io.setClock

  countReg := nextCount
  maxReachedReg := nextMaxReached
  pwmReg := nextPwm

  // ###################
  // Output
  // ###################
  io.count := countReg
  io.maxReached := maxReachedReg
  io.pwm := pwmReg



  // ###################
  // Module implementation
  // ###################

  val countSum = WireInit(0.U((params.countWidth).W))
  countSum := countReg + prescalerReg

  val countOverflow = WireInit(false.B)
  countOverflow := (countSum < countReg) || (countSum < prescalerReg)

  when(enReg) {
    when(setClockReg) {
        nextCount := setClockValueReg
        nextMaxReached := false.B
        nextPwm := false.B
    }.otherwise(
      {
        // If the countSum is greater than the maxCount, then reset the count to 0, the max has been reached
        // also reset the count if the countSum overflows, this means that the maxMust be reached as well
        when(countSum >= maxCountReg || countOverflow) {
          nextCount := 0.U
          nextMaxReached := true.B
        }.otherwise {
          nextCount := countReg + prescalerReg
          nextMaxReached := false.B
        }
        nextPwm := countReg >= pwmCeilingReg
      }
    )
  }.otherwise {
    nextCount := countReg
    nextMaxReached := maxReachedReg
    nextPwm := pwmReg
  }




  // ###################
  // Formal verification
  // ###################
  if (formal) {
    // Formal Verification Vars
    when(enReg) {
      // ######################
      // Liveness Specification
      // ######################
      // assert that every cycle,
      // (prescaler > 0) implies ((nextCount > countReg) or (nextMaxReached))
      val madeProgressFV = (nextCount > countReg)
      val maxReachedFV = nextMaxReached

      // When setClock is asserted, nextCount should be set to setClockValue
      when(setClockReg) {
        assert(nextCount === setClockValueReg)
        assert(!nextMaxReached) // maxReached should be false when setting the clock
        assert(!nextPwm)        // pwm should be false when setting the clock
      }.otherwise {
        // When setClock is not asserted, the timer should behave as expected
        when(prescalerReg > 0.U) {
          assert(madeProgressFV || maxReachedFV)
        }
      }
    }
  }
}