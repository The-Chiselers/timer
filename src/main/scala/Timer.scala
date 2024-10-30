// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.Timer

import java.io.File
import java.io.PrintWriter

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class Timer(p: BaseParams) extends Module {
  val io = IO(new Bundle {
    val apb = new ApbInterface(p)
    val pins = new Bundle {
      val irqOutput = Output(UInt(1.W))
      val event     = Input(UInt(1.W))
    }
  })

  val regs = new TimerRegs(p)

  // Temporary Initialization
  io.apb.PSLVERR    := false.B
  io.pins.irqOutput := false.B

  // APB
  io.apb.PRDATA := 0.U // Needs to be initialized

  when(io.apb.PSEL && io.apb.PENABLE) {
    when(io.apb.PWRITE) { // Write Operation
      registerDecodeWrite(io.apb.PADDR)
      io.apb.PREADY := true.B
    }.otherwise { // Read Operation
      registerDecodeRead(io.apb.PADDR)
      io.apb.PREADY := true.B
    }
  }.otherwise(io.apb.PREADY := false.B)

  // function to take addr and data from APB and write to gpio register space and or child modules if applicable
  def registerDecodeWrite(addr: UInt): Unit = {

    when(
      addr >= regs.COUNT_ADDR.U && addr <= regs.COUNT_ADDR_MAX.U
    ) {
      printf(
        "Writing COUNT Register, data: %x, addr: %x\n",
        io.apb.PWDATA,
        addr
      )
      val shiftAddr = addr - regs.COUNT_ADDR.U
      regs.COUNT(shiftAddr) := io.apb.PWDATA
    }
    when(
      addr >= regs.VALUE_ADDR.U && addr <= regs.VALUE_ADDR_MAX.U
    ) {
      printf(
        "Writing VALUE Register, data: %x, addr: %x\n",
        io.apb.PWDATA,
        addr
      )
      val shiftAddr = addr - regs.VALUE_ADDR_MAX.U
      regs.VALUE(shiftAddr) := io.apb.PWDATA
    }
  }

  def registerDecodeRead(addr: UInt): Unit = {
    when(
      addr >= regs.COUNT_ADDR.U && addr <= regs.COUNT_ADDR_MAX.U
    ) {
      printf(
        "Reading COUNT Register, data: %x, addr: %x\n",
        regs.readCount(),
        addr
      )
      val shiftAddr = addr - regs.COUNT_ADDR.U
      io.apb.PRDATA := regs.COUNT(shiftAddr)
    }
    when(
      addr >= regs.VALUE_ADDR.U && addr <= regs.VALUE_ADDR_MAX.U
    ) {
      printf(
        "Reading VALUE Register, data: %x, addr: %x\n",
        regs.readCount(),
        addr
      )
      val shiftAddr = addr - regs.VALUE_ADDR.U
      io.apb.PRDATA := regs.VALUE(shiftAddr)
    }
  }
}
