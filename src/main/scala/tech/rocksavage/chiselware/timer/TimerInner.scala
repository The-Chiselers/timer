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

  val prescalerCount = RegInit(0.U(params.countWidth.W))
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

  val prescalerModule = Module(new TimerPrescaler(params, formal))
  prescalerModule.io.maxCount := io.prescaler
  prescalerModule.io.en := io.en


  // ###################
  // Module implementation
  // ###################

  when(io.en) {
    when(countReg + io.prescaler >= io.maxCount) {
      nextCount := 0.U
      nextMaxReached := true.B
    }.otherwise {
      nextCount := countReg + prescalerModule.io.maxReached
      nextMaxReached := false.B
    }
  }.otherwise(
    {
      countReg := countReg
      nextMaxReached := maxReachedReg
    }
  )


  // ###################
  // Formal verification
  // ###################
  if (formal) {
    // Formal Verification Vars

    val cyclesSinceLastMaxReached = RegInit(0.U((2 * params.countWidth).W))
    when(io.maxReached) {
      cyclesSinceLastMaxReached := 0.U
    }.otherwise {
      cyclesSinceLastMaxReached := cyclesSinceLastMaxReached + 1.U
    }

    val prevPrescalerValues = RegInit(VecInit(Seq.fill(params.countWidth)(0.U(params.countWidth.W))))
    val prevMaxCountValues = RegInit(VecInit(Seq.fill(params.countWidth)(0.U(params.countWidth.W))))
    val prevCountValues = RegInit(VecInit(Seq.fill(params.countWidth)(0.U(params.countWidth.W))))
    val prevMaxReachedValues = RegInit(VecInit(Seq.fill(params.countWidth)(false.B)))

    when(io.en) {
      for (i <- 1 until params.countWidth) {
        prevPrescalerValues(i) := prevPrescalerValues(i - 1)
        prevMaxCountValues(i) := prevMaxCountValues(i - 1)
        prevCountValues(i) := prevCountValues(i - 1)
        prevMaxReachedValues(i) := prevMaxReachedValues(i - 1)
      }
      prevPrescalerValues(0) := io.prescaler
      prevMaxCountValues(0) := io.maxCount
      prevCountValues(0) := io.count
      prevMaxReachedValues(0) := io.maxReached
    }


    when(io.en) {
      // ########
      // Liveness
      // ########

      // assert that cycles since last max reached is always less than the max count (worst case scenario)
      assert(cyclesSinceLastMaxReached < params.countWidth.U, "cyclesSinceLastMaxReached < io.maxCount")


    }
  }
}