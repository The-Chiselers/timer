// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.Timer

import java.io.File
import java.io.PrintWriter

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class Counter(p: BaseParams) extends Module {
  val io = IO(new Bundle {
    val increment       = Input(UInt(p.timer0.W))
    val set_count_value = Input(UInt(p.timer0.W))
    val set_count       = Input(Bool())
    val count           = Output(UInt(p.timer0.W))
  })

  val count = RegInit(0.U(p.timer0.W))

  count := count + io.increment

  when(io.set_count) {
    count := io.set_count_value
  }

  io.count := count
}
