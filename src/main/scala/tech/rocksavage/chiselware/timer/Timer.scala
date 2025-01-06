import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbInterface, ApbParams}
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeError, AddrDecodeParams}
import tech.rocksavage.chiselware.timer.bundle.{TimerBundle, TimerInterruptBundle, TimerInterruptEnum, TimerOutputBundle}
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.chiselware.addressable.{AddressableRegister, RegisterMap}
import tech.rocksavage.chiselware.timer.TimerInner

class Timer(
             val timerParams: TimerParams,
           ) extends Module {
  // Default Constructor
  def this() = this(TimerParams())

  val dataWidth = timerParams.dataWidth
  val addressWidth = timerParams.addressWidth

  // Input/Output bundle for the Timer module
  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(dataWidth, addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
  })

  // Create a RegisterMap to manage the addressable registers
  val registerMap = new RegisterMap(dataWidth, addressWidth)

  // Define addressable registers using the macro annotation
  @AddressableRegister
  val prescaler: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val maxCount: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val pwmCeiling: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val setCountValue: UInt = RegInit(0.U(timerParams.countWidth.W))

  @AddressableRegister
  val setCount: Bool = RegInit(false.B)

  // Generate AddrDecode and ApbInterface
  val apbInterface = Module(new ApbInterface(ApbParams(dataWidth, addressWidth)))
  apbInterface.io.apb <> io.apb

  val addrDecodeParams = registerMap.getAddrDecodeParams
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := apbInterface.io.mem.addr
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  apbInterface.io.mem.error := addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange
  apbInterface.io.mem.rdata := 0.U

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

  // Instantiate the TimerInner module
  val timerInner = Module(new TimerInner(timerParams))
  timerInner.io.timerInputBundle.setCount := setCount
  timerInner.io.timerInputBundle.prescaler := prescaler
  timerInner.io.timerInputBundle.maxCount := maxCount
  timerInner.io.timerInputBundle.pwmCeiling := pwmCeiling
  timerInner.io.timerInputBundle.setCountValue := setCountValue

  // Connect the TimerInner outputs to the top-level outputs
  io.timerOutput <> timerInner.io.timerOutputBundle

  // Handle interrupts
  io.interrupt.interrupt := TimerInterruptEnum.None
  when(timerInner.io.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }
}