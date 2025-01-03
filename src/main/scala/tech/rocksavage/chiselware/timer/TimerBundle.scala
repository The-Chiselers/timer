// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.timer

import chisel3._

/**
 * A bundle representing the input and output signals for a timer module.
 *
 * @param params The configuration parameters for the timer, including the width of the count registers.
 */
class TimerBundle(params: TimerParams) extends Bundle {

  /** Enable signal for the timer. When high, the timer is active. */
  val en = Input(Bool())

  /** Prescaler value to divide the clock frequency. */
  val prescaler = Input(UInt(params.countWidth.W))

  /** Maximum count value before the timer resets. */
  val maxCount = Input(UInt(params.countWidth.W))

  /** PWM ceiling value to control the duty cycle of the PWM signal. */
  val pwmCeiling = Input(UInt(params.countWidth.W))

  /** Value to set the clock counter to when `setClock` is asserted. */
  val setClockValue = Input(UInt(params.countWidth.W))

  /** Signal to set the clock counter to `setClockValue`. */
  val setClock = Input(Bool())

  /** Current count value of the timer. */
  val count = Output(UInt(params.countWidth.W))

  /** Signal indicating that the timer has reached its maximum count value. */
  val maxReached = Output(Bool())

  /** PWM output signal with a duty cycle controlled by `pwmCeiling`. */
  val pwm = Output(Bool())
}