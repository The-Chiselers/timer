// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chisel3.util.Cat
import chiseltest.formal.past
import tech.rocksavage.chiselware.timer.bundle.TimerBundle
import tech.rocksavage.chiselware.timer.param.TimerParams

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
class TimerInnerFVHarness(
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
    // Input Regs
    // ###################

    // Regs
    val enReg            = RegInit(false.B)
    val prescalerReg     = RegInit(0.U(params.prescalerWidth.W))
    val maxCountReg      = RegInit(0.U(params.countWidth.W))
    val pwmCeilingReg    = RegInit(0.U(params.countWidth.W))
    val setCountValueReg = RegInit(0.U(params.countWidth.W))
    val setCountReg      = RegInit(false.B)
    val maxCountEnableInterruptReg = RegInit(false.B)

    // Assignment
    enReg            := io.timerInputBundle.en
    prescalerReg     := io.timerInputBundle.prescaler
    maxCountReg      := io.timerInputBundle.maxCount
    pwmCeilingReg    := io.timerInputBundle.pwmCeiling
    setCountValueReg := io.timerInputBundle.setCountValue
    setCountReg      := io.timerInputBundle.setCount
    maxCountEnableInterruptReg := io.timerInputBundle.maxCountEnableInterrupt

    // ###################
    // Output
    // ###################

    // Wires
    val countNext      = WireInit(0.U(params.countWidth.W))
    val maxReachedNext = WireInit(false.B)
    val pwmNext        = WireInit(false.B)
    val maxCountInterruptNext = WireInit(false.B)

    // ###################
    // Module Instantiation
    // ###################

    val timerInner = Module(new TimerInner(params, formal))

    timerInner.io.timerInputBundle.en            := enReg
    timerInner.io.timerInputBundle.prescaler     := prescalerReg
    timerInner.io.timerInputBundle.maxCount      := maxCountReg
    timerInner.io.timerInputBundle.pwmCeiling    := pwmCeilingReg
    timerInner.io.timerInputBundle.setCountValue := setCountValueReg
    timerInner.io.timerInputBundle.setCount      := setCountReg
    timerInner.io.timerInputBundle.maxCountEnableInterrupt := maxCountEnableInterruptReg

    countNext      := timerInner.io.timerOutputBundle.count
    maxReachedNext := timerInner.io.timerOutputBundle.maxReached
    pwmNext        := timerInner.io.timerOutputBundle.pwm
    maxCountInterruptNext := timerInner.io.timerOutputBundle.interrupts.maxCountInterrupt

    io.timerOutputBundle.count      := countNext
    io.timerOutputBundle.maxReached := maxReachedNext
    io.timerOutputBundle.pwm        := pwmNext
    io.timerOutputBundle.interrupts.maxCountInterrupt := maxCountInterruptNext
}
