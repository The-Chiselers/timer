// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chiseltest.formal.past


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

  // ###################
  // Syncronizers for input / formal verify
  // ###################
  val enReg = RegInit(false.B)
  val prescalerReg = RegInit(0.U(params.countWidth.W))
  val maxCountReg = RegInit(0.U(params.countWidth.W))

  // ###################
  // Registers that hold the output values
  // ###################
  val countReg = RegInit(0.U(params.countWidth.W))
  val maxReachedReg = RegInit(false.B)

  // ###################
  // Next state logic
  // ###################
  val nextCount = WireInit(0.U(params.countWidth.W))
  val nextMaxReached = WireInit(false.B)

  // ###################
  // Instatiation
  // ###################

  enReg := io.en
  prescalerReg := io.prescaler
  maxCountReg := io.maxCount

  countReg := nextCount
  maxReachedReg := nextMaxReached

  // ###################
  // Output
  // ###################
  io.count := countReg
  io.maxReached := maxReachedReg



  // ###################
  // Module implementation
  // ###################

  val countSum = WireInit(0.U((params.countWidth).W))
  countSum := countReg + prescalerReg

  val countOverflow = WireInit(false.B)
  countOverflow := (countSum < countReg) || (countSum < prescalerReg)

  when(enReg) {
    when(countSum >= maxCountReg || countOverflow) {
      nextCount := 0.U
      nextMaxReached := true.B
    }.otherwise {
      nextCount := countReg + prescalerReg
      nextMaxReached := false.B
    }
  }.otherwise {
    nextCount := countReg
    nextMaxReached := maxReachedReg
  }




  // ###################
  // Formal verification
  // ###################
  if (formal) {
    // Formal Verification Vars


    when(enReg) {
      // ######################
      // Liveness Specification
      // ######################

      // assert that every cycle,
      // (prescaler > 0) implies ((nextCount > countReg) or (nextMaxReached))

      val madeProgressFV = (nextCount > countReg)
      val maxReachedFV = nextMaxReached

      when(prescalerReg > 0.U) {
        assert(madeProgressFV || maxReachedFV)
      }
    }
  }
}