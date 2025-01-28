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
    val prescalerCounterNext = WireInit(0.U(params.prescalerWidth.W))
    val prescalerWrapNext    = WireInit(false.B)
    val countOverflowNext    = WireInit(false.B)

    // Regs
    val prescalerCounterReg = RegInit(0.U(params.prescalerWidth.W))
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

    val (countNextTemp, countOverflowNextTemp, maxReachedNextTemp) =
        computeCount(
          en = enReg,
          count = countReg,
          maxCount = maxCountReg,
          setCount = setCountReg,
          setCountValue = setCountValueReg,
          prescalerWrap = prescalerWrapReg
        )
    countNext         := countNextTemp
    countOverflowNext := countOverflowNextTemp
    maxReachedNext    := maxReachedNextTemp

    pwmNext := computePwmNext(
      en = enReg,
      count = countReg,
      pwmCeiling = pwmCeilingReg
    )

    prescalerCounterNext := computePrescalerCounterNext(
      en = enReg,
      setCount = setCountReg,
      prescalerCounter = prescalerCounterReg,
      prescaler = prescalerReg
    )

    prescalerWrapNext := computePrescalerWrapNext(
      en = enReg,
      setCount = setCountReg,
      prescalerCounter = prescalerCounterReg,
      prescaler = prescalerReg
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

        val maxCountStable3 = (past(maxCountReg) === maxCountReg) && (past(
          maxCountReg,
          2
        ) === maxCountReg)
        val maxCountStable2 = (maxCountReg === past(maxCountReg))
        val setCountStableLow3 =
            (setCountReg === 0.B) && (past(setCountReg) === 0.B) && (past(
              setCountReg,
              2
            ) === 0.B)
        val prescalerStableLow3 =
            (prescalerReg === 0.U) && (past(prescalerReg) === 0.U) && (past(
              prescalerReg,
              2
            ) === 0.U)
        val enableStable3 =
            (enReg === past(enReg)) && (past(enReg, 2) === past(enReg))

        when(enReg) {
            // ######################
            // Liveness Specification
            // ######################
            // assert that every cycle,
            // (prescaler > 0) implies ((nextCount > countReg) or (nextMaxReached))
            when(
              maxCountStable3 && setCountStableLow3 && prescalerStableLow3 && enableStable3
            ) {
                assert(madeProgressFV || maxReachedFV)
            }

            // ######################
            // Clock Specification
            // ######################
            // assert that every cycle, the count is less than the maxCount if the maxCount is stable
            // This is necessary since there is no synchronized maxCountNext as maxCount is a input
            when(maxCountStable3 && setCountStableLow3) {
                assert(countNext <= maxCountReg)
            }

            // ######################
            // Prescaler Specification
            // ######################
            // assert that every cycle, the prescaler is less than the prescalerReg if the prescalerReg is stable
            // This is necessary since there is no synchronized prescalerNext as prescaler is a input
            when(prescalerStableLow3 && setCountStableLow3) {
                assert(prescalerCounterNext <= prescalerReg)
            }

        }
    }

    def computeCount(
        en: Bool,
        maxCount: UInt,
        count: UInt,
        setCount: Bool,
        setCountValue: UInt,
        prescalerWrap: Bool
    ) = {
        val countNextBeforeBoundsCheck = WireInit(0.U(params.countWidth.W))
        val countNext                  = WireInit(0.U(params.countWidth.W))
        val countOverflowNext          = WireInit(false.B)
        val maxReachedNext             = WireInit(false.B)

        // Initial Count Assignment
        when(en) {
            when(setCount) {
                countNextBeforeBoundsCheck := setCountValue
            }.elsewhen(prescalerWrap) {
                when(count >= maxCount) {
                    countNextBeforeBoundsCheck := 0.U
                }.otherwise {
                    countNextBeforeBoundsCheck := count + 1.U
                }
            }.otherwise {
                countNextBeforeBoundsCheck := count
            }
        }.otherwise {
            countNextBeforeBoundsCheck := count
        }

        // Initial Count Overflow Assignment
        when(en) {
            when(setCount) {
                countOverflowNext := false.B
            }.elsewhen(prescalerWrap) {
                countOverflowNext := (countNextBeforeBoundsCheck === 0.U) & prescalerWrap
            }.otherwise {
                countOverflowNext := false.B
            }
        }.otherwise {
            countOverflowNext := false.B
        }

        // Check if the count has reached the maxCount
        when(en) {
            when(setCount) {
                maxReachedNext := false.B
            }.otherwise {
                maxReachedNext := (countNextBeforeBoundsCheck >= maxCount) || countOverflowNext
            }
        }.otherwise {
            maxReachedNext := false.B
        }

        when(en) {
            when(maxReachedNext) {
                countNext := 0.U
            }.otherwise {
                countNext := countNextBeforeBoundsCheck
            }
        }.otherwise {
            countNext := count
        }

        (countNext, countOverflowNext, maxReachedNext)
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
        setCount: Bool,
        prescalerCounter: UInt,
        prescaler: UInt
    ) = {
        val prescalerCounterNext = WireInit(0.U(params.countWidth.W))
        when(en) {
            when(prescalerCounter >= prescaler || setCount) {
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
        setCount: Bool,
        prescalerCounter: UInt,
        prescaler: UInt
    ) = {
        val prescalerWrapNext = WireInit(false.B)
        when(en) {
            prescalerWrapNext := (prescalerCounter === prescaler) || setCount
        }.otherwise {
            prescalerWrapNext := false.B
        }
        prescalerWrapNext
    }
}
