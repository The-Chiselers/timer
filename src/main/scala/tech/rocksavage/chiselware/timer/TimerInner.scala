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
    // Input Regs
    // ###################

    // Regs
    val enReg            = RegInit(false.B)
    val prescalerReg     = RegInit(0.U(params.prescalerWidth.W))
    val maxCountReg      = RegInit(0.U(params.countWidth.W))
    val pwmCeilingReg    = RegInit(0.U(params.countWidth.W))
    val setCountValueReg = RegInit(0.U(params.countWidth.W))
    val setCountReg      = RegInit(false.B)

    // Assignment
    enReg            := io.timerInputBundle.en
    prescalerReg     := io.timerInputBundle.prescaler
    maxCountReg      := io.timerInputBundle.maxCount
    pwmCeilingReg    := io.timerInputBundle.pwmCeiling
    setCountValueReg := io.timerInputBundle.setCountValue
    setCountReg      := io.timerInputBundle.setCount

    // ###################
    // Output
    // ###################

    // Wires
    val countNext      = WireInit(0.U(params.countWidth.W))
    val maxReachedNext = WireInit(false.B)
    val pwmNext        = WireInit(false.B)

    // Regs
    val countReg      = RegInit(0.U(params.countWidth.W))
    val maxReachedReg = RegInit(false.B)
    val pwmReg        = RegInit(false.B)

    // Assignment
    countReg      := countNext
    maxReachedReg := maxReachedNext
    pwmReg        := pwmNext

    io.timerOutputBundle.count      := countReg
    io.timerOutputBundle.maxReached := maxReachedReg
    io.timerOutputBundle.pwm        := pwmReg

    // ###################
    // Internal
    // ###################

    // Wires
    val prescalerCounterNext = WireInit(0.U(params.countWidth.W))
    val prescalerWrapNext    = WireInit(false.B)
    val countOverflowNext    = WireInit(false.B)

    // Regs
    val prescalerCounterReg = RegInit(0.U(params.countWidth.W))
    val prescalerWrapReg    = RegInit(false.B)
    val countOverflowReg    = RegInit(false.B)

    // Regs Assignment
    prescalerCounterReg := prescalerCounterNext
    prescalerWrapReg    := prescalerWrapNext
    countOverflowReg    := countOverflowNext

    // ###################
    // FSM
    // - Only assigning Internal and Output Next values
    // ###################

    countNext := computeCountNext(
      en = enReg,
      count = countReg,
      setCount = setCountReg,
      setCountValue = setCountValueReg,
      prescalerWrap = prescalerWrapReg
    )

    maxReachedNext := computeMaxReachedNext(
      en = enReg,
      count = countReg,
      countNext = countNext,
      maxCount = maxCountReg,
      setCount = setCountReg,
      setCountValue = setCountValueReg,
      prescalerWrap = prescalerWrapReg
    )

    pwmNext := computePwmNext(
      en = enReg,
      count = countReg,
      pwmCeiling = pwmCeilingReg
    )

    prescalerCounterNext := computePrescalerCounterNext(
      en = enReg,
      prescalerCounter = prescalerCounterReg,
      prescaler = prescalerReg
    )

    prescalerWrapNext := computePrescalerWrapNext(
      en = enReg,
      prescalerCounter = prescalerCounterReg,
      prescaler = prescalerReg
    )

    countOverflowNext := computeCountOverflowNext(
      en = enReg,
      count = countReg,
      setCount = setCountReg,
      setCountValue = setCountValueReg,
      prescalerWrap = prescalerWrapReg
    )

    // ###################
    // Formal verification
    // ###################
    if (formal) {
        // Formal Verification Vars
        val combinedTimer     = WireInit(0.U((2 * params.countWidth).W))
        val combinedTimerNext = WireInit(0.U((2 * params.countWidth).W))
        // both prescaler and countReg
        combinedTimer     := Cat(countReg, prescalerCounterReg)
        combinedTimerNext := Cat(countNext, prescalerCounterNext)

        val madeProgressFV = (combinedTimerNext > combinedTimer)
        val maxReachedFV   = maxReachedNext

        when(enReg) {
            // ######################
            // Liveness Specification
            // ######################
            // assert that every cycle,
            // (prescaler > 0) implies ((nextCount > countReg) or (nextMaxReached))
            when(!setCountReg) {
                assert(madeProgressFV || maxReachedFV)
            }

            // ######################
            // Clock Specification
            // ######################
            // assert that every cycle, the count is less than the maxCount
            assert(countReg < maxCountReg)

            // ######################
            // Prescaler Specification
            // ######################
            // assert that every cycle, the prescaler is less than the prescalerReg
            assert(prescalerCounterReg < prescalerReg)

        }
    }

    def computeCountNext(
        en: Bool,
        count: UInt,
        setCount: Bool,
        setCountValue: UInt,
        prescalerWrap: Bool
    ) = {
        val countNext = WireInit(0.U(params.countWidth.W))
        when(en) {
            when(setCount) {
                countNext := setCountValue
            }.otherwise {
                when(prescalerWrap) {
                    countNext := count + 1.U
                }.otherwise {
                    countNext := count
                }
            }
        }.otherwise {
            countNext := count
        }
        countNext
    }

    def computeMaxReachedNext(
        en: Bool,
        count: UInt,
        countNext: UInt,
        maxCount: UInt,
        setCount: Bool,
        setCountValue: UInt,
        prescalerWrap: Bool
    ) = {
        val maxReachedNext = WireInit(false.B)
        when(en) {
            when(setCount) {
                maxReachedNext := false.B
            }.otherwise {
                when(prescalerWrap) {
                    when(
                      countNext >= maxCount || (countNext === 0.U && prescalerWrap)
                    ) {
                        maxReachedNext := true.B
                    }.otherwise {
                        maxReachedNext := false.B
                    }
                }.otherwise {
                    maxReachedNext := false.B
                }
            }
        }.otherwise {
            maxReachedNext := false.B
        }
        maxReachedNext
    }

    def computePwmNext(
        en: Bool,
        count: UInt,
        pwmCeiling: UInt
    ) = {
        val pwmNext = WireInit(false.B)
        when(en) {
            pwmNext := count >= pwmCeiling
        }.otherwise {
            pwmNext := false.B
        }
        pwmNext
    }

    def computePrescalerCounterNext(
        en: Bool,
        prescalerCounter: UInt,
        prescaler: UInt
    ) = {
        val prescalerCounterNext = WireInit(0.U(params.countWidth.W))
        when(en) {
            when(prescalerCounter === prescaler) {
                prescalerCounterNext := 0.U
            }.otherwise {
                prescalerCounterNext := prescalerCounter + 1.U
            }
        }.otherwise {
            prescalerCounterNext := prescalerCounter
        }
        prescalerCounterNext
    }

    def computePrescalerWrapNext(
        en: Bool,
        prescalerCounter: UInt,
        prescaler: UInt
    ) = {
        val prescalerWrapNext = WireInit(false.B)
        when(en) {
            when(prescalerCounter === prescaler) {
                prescalerWrapNext := true.B
            }.otherwise {
                prescalerWrapNext := false.B
            }
        }.otherwise {
            prescalerWrapNext := false.B
        }
        prescalerWrapNext
    }

    def computeCountOverflowNext(
        en: Bool,
        count: UInt,
        setCount: Bool,
        setCountValue: UInt,
        prescalerWrap: Bool
    ) = {
        val countOverflowNext = WireInit(false.B)
        when(en) {
            when(setCount) {
                countOverflowNext := false.B
            }.otherwise {
                countOverflowNext := (count === 0.U) && prescalerWrap
            }
        }.otherwise {
            countOverflowNext := false.B
        }
        countOverflowNext
    }
}
