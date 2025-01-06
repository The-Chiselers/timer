import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.clock.param.ClockParams
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbInterface, ApbParams}
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeError, AddrDecodeParams}
import tech.rocksavage.chiselware.timer.bundle.{TimerBundle, TimerInterruptBundle, TimerInterruptEnum, TimerOutputBundle}
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.chiselware.addressable.{AddressableRegister, RegisterMap}
import tech.rocksavage.chiselware.timer.TimerClocked

class Timer(
             val timerParams: TimerParams,
             val clockParams: ClockParams
           ) extends Module {
  // Default Constructor
  def this() = this(TimerParams(), ClockParams())

  val dataWidth = timerParams.dataWidth
  val addressWidth = timerParams.addressWidth

  // Input/Output bundle for the Timer module
  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(dataWidth, addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
    val clocks = Vec(clockParams.numClocks, Clock())
  })

  // Create a RegisterMap to manage the addressable registers
  val registerMap = new RegisterMap(dataWidth, addressWidth)

  // Define addressable registers using the macro annotation
  @AddressableRegister
  val setClock: Bool = RegInit(false.B)

  @AddressableRegister
  val prescaler: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val maxCount: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val pwmCeiling: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val setClockValue: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val clockSelect: UInt = RegInit(0.U(log2Ceil(clockParams.numClocks).W))

  // Generate AddrDecode and ApbInterface
  val addrDecodeParams = registerMap.getAddrDecodeParams
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := io.apb.PADDR
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  val apbInterface = Module(new ApbInterface(ApbParams(dataWidth, addressWidth)))
  apbInterface.io.apb <> io.apb

  // Connect the memory interface of ApbInterface to the registers
  apbInterface.io.mem.addr := addrDecode.io.addrOut
  apbInterface.io.mem.wdata := io.apb.PWDATA
  apbInterface.io.mem.read := !io.apb.PWRITE
  apbInterface.io.mem.write := io.apb.PWRITE

  // Handle writes to the registers
  when(apbInterface.io.mem.write) {
    for (reg <- registerMap.getRegisters) {
      when(addrDecode.io.sel(reg.id)) {
        reg.writeCallback(apbInterface.io.mem.wdata)
      }
    }
  }

  // Handle reads from the registers
  when(apbInterface.io.mem.read) {
    apbInterface.io.mem.rdata := 0.U
    for (reg <- registerMap.getRegisters) {
      when(addrDecode.io.sel(reg.id)) {
        apbInterface.io.mem.rdata := reg.readCallback()
      }
    }
  }

  // Instantiate the TimerClocked module
  val timerInner = Module(new TimerClocked(timerParams, clockParams))
  timerInner.io.timerBundle.timerInputBundle.setClock := setClock
  timerInner.io.timerBundle.timerInputBundle.prescaler := prescaler
  timerInner.io.timerBundle.timerInputBundle.maxCount := maxCount
  timerInner.io.timerBundle.timerInputBundle.pwmCeiling := pwmCeiling
  timerInner.io.timerBundle.timerInputBundle.setClockValue := setClockValue
  timerInner.io.clockBundle.clockSel := clockSelect

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