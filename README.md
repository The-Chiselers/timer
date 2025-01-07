# APB Library

## Overview

The `APB Library` provides a set of tools and modules for implementing Advanced Peripheral Bus (APB) interfaces in hardware designs using Chisel. The library includes modules for defining APB bundles, managing address decoding, and integrating with other hardware components. It simplifies the process of creating memory-mapped I/O systems and ensures compliance with the APB protocol.

## Features

- **APB Bundle Definition**: Easily define APB bundles with configurable data and address widths.
- **Address Decoding**: Automatically decode memory addresses and manage memory ranges.
- **Error Handling**: Detect and handle address out-of-range errors.
- **Integration with RegisterMap**: Seamlessly integrate with the `RegisterMap` module for managing addressable registers.

## Usage

### Defining APB Bundles

To define an APB bundle, use the `ApbBundle` class. This class takes an `ApbParams` object as a parameter, which specifies the data and address widths.

```scala
class ApbBundle(p: ApbParams) extends Bundle {
  val PSEL    = Input(Bool())                 // Peripheral select
  val PENABLE = Input(Bool())                 // Enable signal
  val PWRITE  = Input(Bool())                 // Read/Write signal
  val PADDR   = Input(UInt(p.PADDR_WIDTH.W))  // Address
  val PWDATA  = Input(UInt(p.PDATA_WIDTH.W))  // Write data
  val PRDATA  = Output(UInt(p.PDATA_WIDTH.W)) // Read data
  val PREADY  = Output(Bool())                // Ready signal
  val PSLVERR = Output(Bool())                // Slave error signal
}
```

### Configuring APB Parameters

The `ApbParams` case class allows you to configure the data and address widths for the APB bus.

```scala
case class ApbParams(
  PDATA_WIDTH: Int = 32,
  PADDR_WIDTH: Int = 32
) {
  require(PDATA_WIDTH >= 1, "PDATA_WIDTH must be at least 1 bit")
  require(PADDR_WIDTH >= 1, "PADDR_WIDTH must be at least 1 bit")
}
```

## Example

The following example demonstrates how to use the APB library in a timer design.

```scala
class Timer(val timerParams: TimerParams) extends Module {
  val dataWidth = timerParams.dataWidth
  val addressWidth = timerParams.addressWidth

  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(dataWidth, addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
  })

  val registerMap = new RegisterMap(dataWidth, addressWidth)

  val en: Bool = RegInit(false.B)
  registerMap.createAddressableRegister(en, "en")

  val prescaler: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(prescaler, "prescaler")

  val maxCount: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(maxCount, "maxCount")

  val pwmCeiling: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(pwmCeiling, "pwmCeiling")

  val setCountValue: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(setCountValue, "setCountValue")

  val setCount: Bool = RegInit(false.B)
  registerMap.createAddressableRegister(setCount, "setCount")

  val addrDecodeParams = registerMap.getAddrDecodeParams
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := io.apb.PADDR
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  io.apb.PREADY := (io.apb.PENABLE && io.apb.PSEL)
  io.apb.PSLVERR := addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange

  io.apb.PRDATA := 0.U
  when(io.apb.PSEL && io.apb.PENABLE) {
    when(io.apb.PWRITE) {
      for (reg <registerMap.getRegisters) {
        when(addrDecode.io.sel(reg.id)) {
          reg.writeCallback(addrDecode.io.addrOffset, io.apb.PWDATA)
        }
      }
    }.otherwise {
      for (reg <registerMap.getRegisters) {
        when(addrDecode.io.sel(reg.id)) {
          io.apb.PRDATA := reg.readCallback(addrDecode.io.addrOffset)
        }
      }
    }
  }

  val timerInner = Module(new TimerInner(timerParams))
  timerInner.io.timerInputBundle.en := en
  timerInner.io.timerInputBundle.setCount := setCount
  timerInner.io.timerInputBundle.prescaler := prescaler
  timerInner.io.timerInputBundle.maxCount := maxCount
  timerInner.io.timerInputBundle.pwmCeiling := pwmCeiling
  timerInner.io.timerInputBundle.setCountValue := setCountValue

  io.timerOutput <> timerInner.io.timerOutputBundle

  io.interrupt.interrupt := TimerInterruptEnum.None
  when(timerInner.io.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }
}
```

## Conclusion

The `APB Library` is a powerful tool for implementing APB interfaces in Chisel-based hardware designs. It simplifies the process of defining APB bundles, managing address decoding, and integrating with other hardware components. The library ensures compliance with the APB protocol and provides robust error handling for memory-mapped I/O operations.