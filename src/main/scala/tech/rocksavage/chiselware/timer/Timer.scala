package tech.rocksavage.chiselware.timer

import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.clock.bundle.ClockBundle
import tech.rocksavage.chiselware.clock.param.ClockParams
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbInterface, ApbParams}
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeError, AddrDecodeParams}
import tech.rocksavage.chiselware.addressable.APBInterfaceGenerator.generateAPBInterface
import tech.rocksavage.chiselware.timer.bundle.{TimerBundle, TimerInterruptBundle, TimerInterruptEnum, TimerOutputBundle}
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.chiselware.addressable.{AddressableModule, AddressableRegister}

class Timer(
             val timerParams: TimerParams,
             val clockParams: ClockParams
           ) extends Module {

  // Define annotations for addressable registers and modules
  @AddressableRegister
  val setClock = RegInit(false.B)

  @AddressableRegister
  val prescaler = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val maxCount = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val pwmCeiling = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val setClockValue = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val clockSelect = RegInit(0.U(log2Ceil(clockParams.numClocks).W))

  @AddressableModule
  val innerModule = Module(new TimerClocked(timerParams, clockParams))

  // Generate APB interface and memorySizes using the macro
  val (_, memorySizes) = generateAPBInterface(this)

  // Derive AddrDecodeParams from memorySizes
  val addrDecodeParams = AddrDecodeParams(
    dataWidth = timerParams.dataWidth,
    addressWidth = timerParams.addressWidth,
    memorySizes = memorySizes
  )

  val apbParams = ApbParams(
    PDATA_WIDTH = timerParams.dataWidth,
    PADDR_WIDTH = timerParams.addressWidth
  )

  // Input/Output bundle for the Timer module
  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(timerParams.dataWidth, timerParams.addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
    val clocks = Vec(clockParams.numClocks, Clock())
  })

  // Instantiate the AddrDecode module
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := io.apb.PADDR
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  // Instantiate the ApbInterface module
  val apbInterface = Module(new ApbInterface(apbParams))
  apbInterface.io.apb <> io.apb

  // Connect the TimerClocked outputs to the top-level outputs
  io.timerOutput <> innerModule.io.timerBundle.timerOutputBundle

  // Handle interrupts
  io.interrupt.interrupt := TimerInterruptEnum.None
  when(innerModule.io.timerBundle.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }

  // Handle APB error conditions
  when(addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange) {
    apbInterface.io.apb.PSLVERR := true.B
  }.otherwise {
    apbInterface.io.apb.PSLVERR := false.B
  }
}