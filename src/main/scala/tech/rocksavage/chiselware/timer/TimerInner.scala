// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chisel3.util.Cat
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
    val enReg            = RegInit(false.B)
    val prescalerReg     = RegInit(0.U(params.countWidth.W))
    val maxCountReg      = RegInit(0.U(params.countWidth.W))
    val pwmCeilingReg    = RegInit(0.U(params.countWidth.W))
    val setCountValueReg = RegInit(0.U(params.countWidth.W))
    val setCountReg      = RegInit(false.B)

    // ###################
    // Registers that hold the output values
    // ###################
    val countReg      = RegInit(0.U(params.countWidth.W))
    val maxReachedReg = RegInit(false.B)
    val pwmReg        = RegInit(false.B)

    // ###################
    // Next state logic
    // ###################
    val nextCount      = WireInit(0.U(params.countWidth.W))
    val nextMaxReached = WireInit(false.B)
    val nextPwm        = WireInit(false.B)

    // ###################
    // Instatiation
    // ###################

    enReg            := io.timerInputBundle.en
    prescalerReg     := io.timerInputBundle.prescaler
    maxCountReg      := io.timerInputBundle.maxCount
    pwmCeilingReg    := io.timerInputBundle.pwmCeiling
    setCountValueReg := io.timerInputBundle.setCountValue
    setCountReg      := io.timerInputBundle.setCount

    countReg      := nextCount
    maxReachedReg := nextMaxReached
    pwmReg        := nextPwm

    // ###################
    // Output
    // ###################
    io.timerOutputBundle.count      := countReg
    io.timerOutputBundle.maxReached := maxReachedReg
    io.timerOutputBundle.pwm        := pwmReg

    // ###################
    // Module implementation
    // ###################

    val countSum      = WireInit(0.U((params.countWidth).W))
    val countOverflow = WireInit(false.B)

    // New prescaler counter register
    val prescalerCounterReg  = RegInit(0.U(params.countWidth.W))
    val prescalerCounterNext = WireInit(0.U(params.countWidth.W))
    val prescalerWrap        = prescalerCounterReg === prescalerReg

    when(prescalerCounterReg === prescalerReg) {
        prescalerCounterNext := 0.U
    }.otherwise {
        prescalerCounterNext := prescalerCounterReg + 1.U
    }

    when(enReg) {
        prescalerCounterReg := prescalerCounterNext
        when(setCountReg) {
            countSum := setCountValueReg
        }.otherwise {
            when(prescalerWrap) {
                countSum := countReg + 1.U
            }.otherwise {
                countSum := countReg
            }
        }
    }.otherwise {
        prescalerCounterReg := prescalerCounterReg
        countSum            := countReg
    }

    // Overflow detection (now based on single increment)
    when(setCountReg) {
        countOverflow := false.B
    }.otherwise {
        countOverflow := (countSum === 0.U) && prescalerWrap
    }

    // State transition logic
    when(prescalerWrap && !setCountReg) {
        when(countSum >= maxCountReg || countOverflow) {
            nextCount      := 0.U
            nextMaxReached := true.B
        }.otherwise {
            nextCount      := countSum
            nextMaxReached := false.B
        }
    }.otherwise {
        when(setCountReg) {
            nextCount      := setCountValueReg
            nextMaxReached := false.B
        }.otherwise {
            nextCount      := countReg
            nextMaxReached := maxReachedReg
        }
    }
    nextPwm := countReg >= pwmCeilingReg

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

            val combinedTimer     = WireInit(0.U((2 * params.countWidth).W))
            val combinedTimerNext = WireInit(0.U((2 * params.countWidth).W))
            // both prescaler and countReg
            combinedTimer     := Cat(countReg, prescalerCounterReg)
            combinedTimerNext := Cat(nextCount, prescalerCounterNext)

            val madeProgressFV = (combinedTimerNext > combinedTimer)
            val maxReachedFV   = nextMaxReached

            when(!setCountReg) {
                assert(madeProgressFV || maxReachedFV)
            }
        }
    }
}
