// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chisel3.util.Cat
import chiseltest.formal.past
import tech.rocksavage.chiselware.timer.bundle.TimerBundle
import tech.rocksavage.chiselware.timer.param.TimerParams

/** A Timer module that implements a configurable timer with prescaler, count,
  * and PWM functionality.
  *
  * @assume
  *   This module does not buffer the input signals. It is assumed that the
  *   input signals are already synchronized.
  *
  * @constructor
  *   Create a new timer module.
  * @param params
  *   TimerParams object including dataWidth, addressWidth, countWidth, and
  *   prescalerWidth.
  * @param formal
  *   A boolean value to enable formal verification.
  * @author
  *   Warren Savage
  */
class TimerInner(
    params: TimerParams,
    formal: Boolean = false
) extends Module {

    val io = IO(new TimerBundle(params))

    // ###################
    // Input Regs
    // ###################

    // Wires for input signals
    val enWire            = WireInit(false.B)
    val prescalerWire     = WireInit(0.U(params.prescalerWidth.W))
    val maxCountWire      = WireInit(0.U(params.countWidth.W))
    val pwmCeilingWire    = WireInit(0.U(params.countWidth.W))
    val setCountValueWire = WireInit(0.U(params.countWidth.W))
    val setCountWire      = WireInit(false.B)

    // Assign input signals to wires
    enWire            := io.timerInputBundle.en
    prescalerWire     := io.timerInputBundle.prescaler
    maxCountWire      := io.timerInputBundle.maxCount
    pwmCeilingWire    := io.timerInputBundle.pwmCeiling
    setCountValueWire := io.timerInputBundle.setCountValue
    setCountWire      := io.timerInputBundle.setCount

    // ###################
    // Output
    // ###################

    // Wires for output signals
    val countNext      = WireInit(0.U(params.countWidth.W))
    val maxReachedNext = WireInit(false.B)
    val pwmNext        = WireInit(false.B)

    // Registers for output signals
    val countReg      = RegInit(0.U(params.countWidth.W))
    val maxReachedReg = RegInit(false.B)
    val pwmReg        = RegInit(false.B)

    // Assign next values to registers
    countReg      := countNext
    maxReachedReg := maxReachedNext
    pwmReg        := pwmNext

    // Assign registers to output bundle
    io.timerOutputBundle.count      := countReg
    io.timerOutputBundle.maxReached := maxReachedReg
    io.timerOutputBundle.pwm        := pwmReg

    // ###################
    // Internal
    // ###################

    // Wires for internal signals
    val prescalerCounterNext = WireInit(0.U(params.prescalerWidth.W))
    val prescalerWrapNext    = WireInit(false.B)
    val countOverflowNext    = WireInit(false.B)

    // Registers for internal signals
    val prescalerCounterReg = RegInit(0.U(params.prescalerWidth.W))
    val prescalerWrapReg    = RegInit(false.B)
    val countOverflowReg    = RegInit(false.B)

    // Assign next values to internal registers
    prescalerCounterReg := prescalerCounterNext
    prescalerWrapReg    := prescalerWrapNext
    countOverflowReg    := countOverflowNext

    // ###################
    // FSM
    // - Only assigning Internal and Output Next values
    // ###################

    // Compute next values for count, count overflow, and max reached
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

    // Compute next value for PWM
    pwmNext := computePwmNext(
      en = enWire,
      count = countReg,
      pwmCeiling = pwmCeilingWire
    )

    // Compute next value for prescaler counter
    prescalerCounterNext := computePrescalerCounterNext(
      en = enWire,
      setCount = setCountWire,
      prescalerCounter = prescalerCounterReg,
      prescaler = prescalerWire
    )

    // Compute next value for prescaler wrap
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

        // Combine countReg and prescalerCounterReg for formal verification
        combinedTimer     := Cat(countReg, prescalerCounterReg)
        combinedTimerNext := Cat(countNext, prescalerCounterNext)

        // Formal verification signals
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
            // Assert that every cycle, (prescaler > 0) implies ((nextCount > countReg) or (nextMaxReached))
            when(
              maxCountStable3 && setCountStableLow3 && prescalerStableLow3 && enableStable3
            ) {
                assert(madeProgressFV || maxReachedFV)
            }

            // ######################
            // Clock Specification
            // ######################
            // Assert that every cycle, the count is less than the maxCount if the maxCount is stable
            when(maxCountStable3 && setCountStableLow3) {
                assert(countNext <= maxCountWire)
            }

            // ######################
            // Prescaler Specification
            // ######################
            // Assert that every cycle, the prescaler is less than the prescalerReg if the prescalerReg is stable
            when(prescalerStableLow3 && setCountStableLow3) {
                assert(prescalerCounterNext <= prescalerWire)
            }
        }
    }

    /** Computes the next value for the count, count overflow, and max reached
      * signals.
      *
      * @param en
      *   Enable signal for the timer.
      * @param maxCount
      *   Maximum count value.
      * @param count
      *   Current count value.
      * @param setCount
      *   Signal to set the count to a specific value.
      * @param setCountValue
      *   Value to set the count to if setCount is true.
      * @param prescalerWrap
      *   Signal indicating that the prescaler has wrapped around.
      * @return
      *   A tuple containing the next count value, count overflow signal, and
      *   max reached signal.
      */
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
            when(setCount) {
                countNextBeforeBoundsCheck := setCountValue
            }.otherwise {
                countNextBeforeBoundsCheck := count
            }
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
            when(setCount) {
                countNext := setCountValue
            }.otherwise {
                countNext := count
            }
        }

        (countNext, countOverflowNext, maxReachedNext)
    }

    /** Computes the next value for the PWM signal.
      *
      * @param en
      *   Enable signal for the timer.
      * @param count
      *   Current count value.
      * @param pwmCeiling
      *   PWM ceiling value.
      * @return
      *   The next value for the PWM signal.
      */
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

    /** Computes the next value for the prescaler counter.
      *
      * @param en
      *   Enable signal for the timer.
      * @param setCount
      *   Signal to set the count to a specific value.
      * @param prescalerCounter
      *   Current prescaler counter value.
      * @param prescaler
      *   Prescaler value.
      * @return
      *   The next value for the prescaler counter.
      */
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

    /** Computes the next value for the prescaler wrap signal.
      *
      * @param en
      *   Enable signal for the timer.
      * @param setCount
      *   Signal to set the count to a specific value.
      * @param prescalerCounter
      *   Current prescaler counter value.
      * @param prescaler
      *   Prescaler value.
      * @return
      *   The next value for the prescaler wrap signal.
      */
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
