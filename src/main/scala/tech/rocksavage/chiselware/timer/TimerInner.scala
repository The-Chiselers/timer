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
class TimerInner(
             params: TimerParams,
             formal: Boolean = false
) extends Module {


  /** Returns the number of memory addresses used by the module
    *
    * @return
    *   The width of the memory
    */
  val io = IO(new Bundle {

    val en = Input(Bool())

    val prescaler = Input(UInt(params.countWidth.W))
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
    when(countReg + io.prescaler >= io.maxCount) {
      nextCount := 0.U
      nextMaxReached := true.B
    }.otherwise {
      nextCount := countReg + io.prescaler
      nextMaxReached := false.B
    }
  }.otherwise {
      countReg := countReg
      nextMaxReached := maxReachedReg
    }



  // ###################
  // Formal verification
  // ###################
  if (formal) {
    // Formal Verification Vars


    when(io.en) {
      // ######################
      // Liveness Specification
      // ######################

      // assert that every cycle,
      // (prescaler > 0) implies ((nextCount > countReg) or (nextMaxReached))
      val madeProgress = (nextCount > countReg)
      val maxReached = maxReachedReg
      when(io.prescaler > 0.U) {
        assert(madeProgress || maxReached)
      }
    }
  }
}