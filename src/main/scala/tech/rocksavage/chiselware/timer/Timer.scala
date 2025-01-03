// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.clock.bundle.ClockBundle
import tech.rocksavage.chiselware.clock.param.ClockParams
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbInterface, ApbParams, MemoryBundle}
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeError, AddrDecodeParams}
import tech.rocksavage.chiselware.timer.bundle.{TimerBundle, TimerInterruptBundle, TimerInterruptEnum, TimerOutputBundle}
import tech.rocksavage.chiselware.timer.param.TimerParams

class Timer(
             val timerParams: TimerParams,
             val clockParams: ClockParams
           ) extends Module {
  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(timerParams.dataWidth, timerParams.addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
    val clocks = Vec(clockParams.numClocks, Clock())
  })

  // Derive ApbParams from timerParams
  val apbParams = ApbParams(timerParams.dataWidth, timerParams.addressWidth)

  // Calculate the memory size for clockSelect
  val clockSelctSize = log2Ceil(clockParams.numClocks)

  // Calculate memory sizes for AddrDecode based on the number of programmable registers in the timer
  val memorySizes = Seq(
    1,                      // en (boolean
    timerParams.countWidth, // prescaler
    timerParams.countWidth, // maxCount
    timerParams.countWidth, // pwmCeiling
    timerParams.countWidth, // setClockValue
    1,                      // setClock (boolean)
    clockSelctSize          // clockSelect

  ).map(size => (size + apbParams.PDATA_WIDTH - 1) / apbParams.PDATA_WIDTH)

  // Derive AddrDecodeParams from timerParams
  val addrDecodeParams = AddrDecodeParams(
    dataWidth = timerParams.dataWidth,
    addressWidth = timerParams.addressWidth,
    memorySizes = memorySizes
  )

  // Instantiate the Vector of Regs used to store the timer input values
  val totalAddresses = memorySizes.sum
  val timerInputRegisterSpace = RegInit(VecInit(Seq.fill(totalAddresses)(0.U(timerParams.countWidth.W))))

  // Instantiate the AddrDecode module
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := io.apb.PADDR
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  // Instantiate the ApbInterface module
  val apbInterface = Module(new ApbInterface(apbParams))
  apbInterface.io.apb <> io.apb

  // Connect the memory interface of ApbInterface to the timerInputRegisterSpace
  apbInterface.io.mem.addr := addrDecode.io.addrOut
  apbInterface.io.mem.wdata := io.apb.PWDATA
  apbInterface.io.mem.read := !io.apb.PWRITE
  apbInterface.io.mem.write := io.apb.PWRITE

  // Handle writes to the timerInputRegisterSpace
  when(apbInterface.io.mem.write) {
    for (i <- 0 until totalAddresses) {
      when(addrDecode.io.sel(i)) {
        timerInputRegisterSpace(i) := apbInterface.io.mem.wdata
      }
    }
  }

  // Handle reads from the timerInputRegisterSpace
  when(apbInterface.io.mem.read) {
    apbInterface.io.mem.rdata := 0.U
    for (i <- 0 until totalAddresses) {
      when(addrDecode.io.sel(i)) {
        apbInterface.io.mem.rdata := timerInputRegisterSpace(i)
      }
    }
  }

  // Instantiate the TimerClocked module
  val timerClocked = Module(new TimerClocked(timerParams, clockParams))
  timerClocked.io.clockBundle.clocks := io.clocks
  timerClocked.io.clockBundle.clockSel := timerInputRegisterSpace(totalAddresses - 1)(log2Ceil(clockParams.numClocks) - 1, 0)

  // Connect the timerInputRegisterSpace to the TimerClocked inputs
  timerClocked.io.timerBundle.timerInputBundle.en := timerInputRegisterSpace(0)(0)
  timerClocked.io.timerBundle.timerInputBundle.prescaler := timerInputRegisterSpace(1)
  timerClocked.io.timerBundle.timerInputBundle.maxCount := timerInputRegisterSpace(2)
  timerClocked.io.timerBundle.timerInputBundle.pwmCeiling := timerInputRegisterSpace(3)
  timerClocked.io.timerBundle.timerInputBundle.setClockValue := timerInputRegisterSpace(4)
  timerClocked.io.timerBundle.timerInputBundle.setClock := timerInputRegisterSpace(5)(0)

  // Connect the TimerClocked outputs to the top-level outputs
  io.timerOutput <> timerClocked.io.timerBundle.timerOutputBundle

  // Handle interrupts
  io.interrupt.interrupt := TimerInterruptEnum.None
  when(timerClocked.io.timerBundle.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }

  // Handle APB error conditions
  when(addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange) {
    apbInterface.io.apb.PSLVERR := true.B
  }.otherwise {
    apbInterface.io.apb.PSLVERR := false.B
  }
}