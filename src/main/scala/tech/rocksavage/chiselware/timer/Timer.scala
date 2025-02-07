// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeError}
import tech.rocksavage.chiselware.addressable.RegisterMap
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbParams}
import tech.rocksavage.chiselware.timer.bundle.TimerOutputBundle
import tech.rocksavage.chiselware.timer.param.TimerParams

/** A Timer module that implements a configurable timer with various
  * functionalities.
  *
  * @param timerParams
  *   Parameters for configuring the timer.
  * @param formal
  *   A boolean value to enable formal verification.
  */
class Timer(val timerParams: TimerParams, formal: Boolean) extends Module {

    /** Data width for the timer */
    val dataWidth = timerParams.dataWidth

    /** Address width for the timer */
    val addressWidth = timerParams.addressWidth

    /** Input/Output bundle for the Timer module */
    val io = IO(new Bundle {

        /** APB interface for the timer */
        val apb = new ApbBundle(ApbParams(dataWidth, addressWidth))

        /** Output bundle for timer outputs */
        val timerOutput = new TimerOutputBundle(timerParams)
    })

    /** RegisterMap to manage the addressable registers */
    val registerMap = new RegisterMap(dataWidth, addressWidth)

    /** Enable signal register */
    val en: Bool = RegInit(false.B)
    registerMap.createAddressableRegister(
      en,
      "en",
      verbose = timerParams.verbose
    )

    /** Prescaler value register */
    val prescaler: UInt = RegInit(0.U(timerParams.prescalerWidth.W))
    registerMap.createAddressableRegister(
      prescaler,
      "prescaler",
      verbose = timerParams.verbose
    )

    /** Maximum count value register */
    val maxCount: UInt = RegInit(0.U(timerParams.countWidth.W))
    registerMap.createAddressableRegister(
      maxCount,
      "maxCount",
      verbose = timerParams.verbose
    )

    /** PWM ceiling value register */
    val pwmCeiling: UInt = RegInit(0.U(timerParams.countWidth.W))
    registerMap.createAddressableRegister(
      pwmCeiling,
      "pwmCeiling",
      verbose = timerParams.verbose
    )

    /** Value to set the count register */
    val setCountValue: UInt = RegInit(0.U(timerParams.countWidth.W))
    registerMap.createAddressableRegister(
      setCountValue,
      "setCountValue",
      verbose = timerParams.verbose
    )

    /** Signal to set the count register */
    val setCount: Bool = RegInit(false.B)
    registerMap.createAddressableRegister(
      setCount,
      "setCount",
      verbose = timerParams.verbose
    )

    /** Enable interrupt for maximum count register */
    val maxCountEnableInterrupt: Bool = RegInit(false.B)
    registerMap.createAddressableRegister(
      maxCountEnableInterrupt,
      "maxCountEnableInterrupt",
      verbose = timerParams.verbose
    )

    // Generate AddrDecode
    /** Parameters for address decoding */
    val addrDecodeParams = registerMap.getAddrDecodeParams

    /** AddrDecode module instance */
    val addrDecode = Module(new AddrDecode(addrDecodeParams))
    addrDecode.io.addr     := io.apb.PADDR
    addrDecode.io.en       := true.B
    addrDecode.io.selInput := true.B
    io.apb.PREADY          := (io.apb.PENABLE && io.apb.PSEL)
    io.apb.PSLVERR := addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange
    io.apb.PRDATA := 0.U

    // Control Register Read/Write
    when(io.apb.PSEL && io.apb.PENABLE) {
        when(io.apb.PWRITE) {
            for (reg <- registerMap.getRegisters) {
                when(addrDecode.io.sel(reg.id)) {
                    reg.writeCallback(addrDecode.io.addrOut, io.apb.PWDATA)
                }
            }
        }.otherwise {
            for (reg <- registerMap.getRegisters) {
                when(addrDecode.io.sel(reg.id)) {
                    io.apb.PRDATA := reg.readCallback(addrDecode.io.addrOut)
                }
            }
        }
    }

    // Instantiate the TimerInner module
    /** TimerInner module instance */
    val timerInner = Module(new TimerInner(timerParams, formal))
    timerInner.io.timerInputBundle.en            := en
    timerInner.io.timerInputBundle.setCount      := setCount
    timerInner.io.timerInputBundle.prescaler     := prescaler
    timerInner.io.timerInputBundle.maxCount      := maxCount
    timerInner.io.timerInputBundle.pwmCeiling    := pwmCeiling
    timerInner.io.timerInputBundle.setCountValue := setCountValue
    timerInner.io.timerInputBundle.maxCountEnableInterrupt := maxCountEnableInterrupt

    // Connect the TimerInner outputs to the top-level outputs
    io.timerOutput <> timerInner.io.timerOutputBundle
}
