// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.clock.bundle

import chisel3._
import chisel3.util.log2Ceil
import tech.rocksavage.chiselware.clock.param.ClockParams

class ClockBundle(params: ClockParams) extends Bundle {

  val clocks = Input(Vec(params.numClocks, Clock()))
  val clockSel = Input(UInt(log2Ceil(params.numClocks).W))

}