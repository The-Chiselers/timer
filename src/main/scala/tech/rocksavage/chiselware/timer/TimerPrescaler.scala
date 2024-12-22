// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._

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
class TimerPrescaler(
  params: TimerParams,
  formal: Boolean = false
) extends Module {

  val io = IO(new Bundle {

    val en = Input(Bool())
    val maxCount = Input(UInt(params.countWidth.W))

    val count = Output(UInt(params.countWidth.W))
    val maxReached = Output(Bool())
  })

  val countReg = RegInit(0.U(params.countWidth.W))
  val maxReachedReg = RegInit(false.B)

  val nextCount = WireInit(0.U(params.countWidth.W))
  val nextMaxReached = WireInit(false.B)

  // ###################
  // Instatiation
  // ###################

  io.count := countReg
  io.maxReached := maxReachedReg

  countReg := nextCount
  maxReachedReg := nextMaxReached

  // ###################
  // Module implementation
  // ###################

  when(io.en) {
    when(countReg === io.maxCount) {
      nextCount := 0.U
      nextMaxReached := true.B
    }.otherwise(
      {
        nextCount := countReg + 1.U
        nextMaxReached := false.B
      }
    )
  }.otherwise({
    nextCount := countReg
    nextMaxReached := maxReachedReg
    }
  )


  // ###################
  // Formal verification
  // ###################
  if (formal) {
  }
}