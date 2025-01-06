import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.clock.bundle.ClockBundle
import tech.rocksavage.chiselware.clock.param.ClockParams
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbInterface, ApbParams}
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeError, AddrDecodeParams}
import tech.rocksavage.chiselware.timer.bundle.{TimerBundle, TimerInterruptBundle, TimerInterruptEnum, TimerOutputBundle}
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.chiselware.addressable.{APBInterfaceGenerator, AddressableRegister}
import tech.rocksavage.chiselware.timer.TimerClocked

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

  val timerInner = Module(new TimerClocked(timerParams, clockParams))
  timerInner.io.timerBundle.timerInputBundle.setClock := setClock
  timerInner.io.timerBundle.timerInputBundle.prescaler := prescaler
  timerInner.io.timerBundle.timerInputBundle.maxCount := maxCount
  timerInner.io.timerBundle.timerInputBundle.pwmCeiling := pwmCeiling
  timerInner.io.timerBundle.timerInputBundle.setClockValue := setClockValue
  timerInner.io.clockBundle.clockSel := clockSelect

  // Generate APB interface and memorySizes using the macro
  val (apbBundle, apbInterface, addrDecode) = APBInterfaceGenerator.generateAPBInterface(this, timerParams.dataWidth, timerParams.addressWidth)

  // Input/Output bundle for the Timer module
  val io = IO(new Bundle {
    val apb = apbBundle
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
    val clocks = Vec(clockParams.numClocks, Clock())
  })

  // Connect the APB interface to the top-level interface

  /* Shouldn't need this, it should be done in the macro */
  //  apbInterface.io.apb <> io.apb

  // Connect the TimerClocked outputs to the top-level outputs
  io.timerOutput <> timerInner.io.timerBundle.timerOutputBundle

  // Handle interrupts
  io.interrupt.interrupt := TimerInterruptEnum.None
  when(timerInner.io.timerBundle.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }

  // Handle APB error conditions
  when(addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange) {
    apbInterface.io.apb.PSLVERR := true.B
  }.otherwise {
    apbInterface.io.apb.PSLVERR := false.B
  }
}