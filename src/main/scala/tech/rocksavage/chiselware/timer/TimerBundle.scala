// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._

class TimerBundle(params: TimerParams) extends Bundle {

  val en = Input(Bool())

  val prescaler = Input(UInt(params.countWidth.W))
  val maxCount = Input(UInt(params.countWidth.W))
  val pwmCeiling = Input(UInt(params.countWidth.W))

  val count = Output(UInt(params.countWidth.W))
  val maxReached = Output(Bool())
  val pwm = Output(Bool())
}