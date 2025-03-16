// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
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
    formal: Boolean = false)
    extends Module {

    /** Returns the number of memory addresses used by the module
      *
      * @return
      *   The width of the memory
      */
    val io: TimerBundle = IO(new TimerBundle(params))

    // ###################
    // Input Regs
    // ###################

    // Regs
    val enReg: Bool                      = RegInit(false.B)
    val prescalerReg: UInt               = RegInit(0.U(params.prescalerWidth.W))
    val maxCountReg: UInt                = RegInit(0.U(params.countWidth.W))
    val pwmCeilingReg: UInt              = RegInit(0.U(params.countWidth.W))
    val setCountValueReg: UInt           = RegInit(0.U(params.countWidth.W))
    val setCountReg: Bool                = RegInit(false.B)
    val maxCountEnableInterruptReg: Bool = RegInit(false.B)

    // Assignment
    enReg                      := io.timerInputBundle.en
    prescalerReg               := io.timerInputBundle.prescaler
    maxCountReg                := io.timerInputBundle.maxCount
    pwmCeilingReg              := io.timerInputBundle.pwmCeiling
    setCountValueReg           := io.timerInputBundle.setCountValue
    setCountReg                := io.timerInputBundle.setCount
    maxCountEnableInterruptReg := io.timerInputBundle.maxCountEnableInterrupt

    // ###################
    // Output
    // ###################

    // Wires
    val countNext: UInt             = WireInit(0.U(params.countWidth.W))
    val maxReachedNext: Bool        = WireInit(false.B)
    val pwmNext: Bool               = WireInit(false.B)
    val maxCountInterruptNext: Bool = WireInit(false.B)

    // ###################
    // Module Instantiation
    // ###################

    val timerInner: TimerInner = Module(new TimerInner(params, formal))

    timerInner.io.timerInputBundle.en                      := enReg
    timerInner.io.timerInputBundle.prescaler               := prescalerReg
    timerInner.io.timerInputBundle.maxCount                := maxCountReg
    timerInner.io.timerInputBundle.pwmCeiling              := pwmCeilingReg
    timerInner.io.timerInputBundle.setCountValue           := setCountValueReg
    timerInner.io.timerInputBundle.setCount                := setCountReg
    timerInner.io.timerInputBundle.maxCountEnableInterrupt := maxCountEnableInterruptReg

    countNext             := timerInner.io.timerOutputBundle.count
    maxReachedNext        := timerInner.io.timerOutputBundle.maxReached
    pwmNext               := timerInner.io.timerOutputBundle.pwm
    maxCountInterruptNext := timerInner.io.timerOutputBundle.interrupts.maxCountInterrupt

    io.timerOutputBundle.count                        := countNext
    io.timerOutputBundle.maxReached                   := maxReachedNext
    io.timerOutputBundle.pwm                          := pwmNext
    io.timerOutputBundle.interrupts.maxCountInterrupt := maxCountInterruptNext
}
