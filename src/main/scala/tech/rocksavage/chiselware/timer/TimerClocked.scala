// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import tech.rocksavage.chiselware.clock.{ClockBundle, ClockParams}

class TimerClocked(
  timerParams: TimerParams,
  clockParams: ClockParams,
  formal: Boolean = false
) extends Module {

  val io = IO(
    new Bundle {
      val timerBundle = new TimerBundle(timerParams)
      val clockBundle = new ClockBundle(clockParams)
    }
  )

  val selectedClock = io.clockBundle.clocks(io.clockBundle.clockSel)
  val timer = withClock(selectedClock) { Module(new TimerInner(timerParams, formal)) }

  io.timerBundle <> timer.io

}