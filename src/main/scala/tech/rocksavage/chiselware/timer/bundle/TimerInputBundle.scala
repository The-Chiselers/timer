// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.timer.bundle

import chisel3._
import tech.rocksavage.chiselware.timer.param.TimerParams

/** A bundle representing the input signals for a timer module.
  *
  * @param params
  *   The configuration parameters for the timer, including the width of the count registers.
  */
class TimerInputBundle(params: TimerParams) extends Bundle {

    /** Enable signal for the timer. When high, the timer is active. */
    val en: Bool = Input(Bool())

    /** Prescaler value to divide the clock frequency. */
    val prescaler: UInt = Input(UInt(params.prescalerWidth.W))

    /** Maximum count value before the timer resets. */
    val maxCount: UInt = Input(UInt(params.countWidth.W))

    /** PWM ceiling value to control the duty cycle of the PWM signal. */
    val pwmCeiling: UInt = Input(UInt(params.countWidth.W))

    /** Value to set the counter to when `setCount` is asserted. */
    val setCountValue: UInt = Input(UInt(params.countWidth.W))

    /** Signal to set the counter to `setCountValue`. */
    val setCount: Bool = Input(Bool())

    /** Signal to enable the interrupt when the counter reaches the maximum count.
      */
    val maxCountEnableInterrupt: Bool = Input(Bool())

}
