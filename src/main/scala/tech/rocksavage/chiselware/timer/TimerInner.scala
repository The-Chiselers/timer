// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chisel3.util.Cat
import chiseltest.formal.past
import tech.rocksavage.chiselware.timer.bundle.TimerBundle
import tech.rocksavage.chiselware.timer.param.TimerParams

/** An Timer modules that
  *
  * @assume
  *   This module does not buffer the input signals
  *   It is assumed that the input signals are already synchronized
  *
  * @constructor
  *   Create a new timer module
  * @param params
  *   TimerParams object including dataWidth and addressWidth, as well as countWidth and prescalerWidth
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
    val enWire            = WireInit(false.B)
    val prescalerWire     = WireInit(0.U(params.prescalerWidth.W))
    val maxCountWire      = WireInit(0.U(params.countWidth.W))
    val pwmCeilingWire    = WireInit(0.U(params.countWidth.W))
    val setCountValueWire = WireInit(0.U(params.countWidth.W))
    val setCountWire      = WireInit(false.B)

    // Assignment
    enWire            := io.timerInputBundle.en
    prescalerWire     := io.timerInputBundle.prescaler
    maxCountWire      := io.timerInputBundle.maxCount
    pwmCeilingWire    := io.timerInputBundle.pwmCeiling
    setCountValueWire := io.timerInputBundle.setCountValue
    setCountWire      := io.timerInputBundle.setCount

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
          en = enWire,
          count = countReg,
          maxCount = maxCountWire,
          setCount = setCountWire,
          setCountValue = setCountValueWire,
          prescalerWrap = prescalerWrapReg
        )
    countNext         := countNextTemp
    countOverflowNext := countOverflowNextTemp
    maxReachedNext    := maxReachedNextTemp

    pwmNext := computePwmNext(
      en = enWire,
      count = countReg,
      pwmCeiling = pwmCeilingWire
    )

    prescalerCounterNext := computePrescalerCounterNext(
      en = enWire,
      setCount = setCountWire,
      prescalerCounter = prescalerCounterReg,
      prescaler = prescalerWire
    )

    prescalerWrapNext := computePrescalerWrapNext(
      en = enWire,
      setCount = setCountWire,
      prescalerCounter = prescalerCounterReg,
      prescaler = prescalerWire
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

        val maxCountStable3 = (past(maxCountWire) === maxCountWire) && (past(
          maxCountWire,
          2
        ) === maxCountWire)
        val setCountStableLow3 =
            (setCountWire === 0.B) && (past(setCountWire) === 0.B) && (past(
              setCountWire,
              2
            ) === 0.B)
        val prescalerStableLow3 =
            (prescalerWire === 0.U) && (past(prescalerWire) === 0.U) && (past(
              prescalerWire,
              2
            ) === 0.U)
        val enableStable3 =
            (enWire === past(enWire)) && (past(enWire, 2) === past(enWire))

        when(enWire) {
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
                assert(countNext <= maxCountWire)
            }

            // ######################
            // Prescaler Specification
            // ######################
            // assert that every cycle, the prescaler is less than the prescalerReg if the prescalerReg is stable
            // This is necessary since there is no synchronized prescalerNext as prescaler is a input
            when(prescalerStableLow3 && setCountStableLow3) {
                assert(prescalerCounterNext <= prescalerWire)
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
