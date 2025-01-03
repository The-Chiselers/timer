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

/** A top-level timer module that integrates a programmable timer with an APB interface.
 *
 * This module provides a configurable timer that can be programmed and monitored via an APB interface.
 * It includes a clock selection mechanism, programmable timer parameters, and interrupt generation.
 *
 * @constructor Create a new Timer module with the specified parameters.
 * @param timerParams Configuration parameters for the timer, including data width, address width, and count width.
 * @param clockParams Configuration parameters for the clock, including the number of available clocks.
 *
 * @example
 * {{{
 * val timerParams = TimerParams(dataWidth = 32, addressWidth = 32, countWidth = 32)
 * val clockParams = ClockParams(numClocks = 4)
 * val timer = Module(new Timer(timerParams, clockParams))
 * }}}
 */
class Timer(
             val timerParams: TimerParams,
             val clockParams: ClockParams
           ) extends Module {

  /** Input/Output bundle for the Timer module.
   *
   * This bundle includes:
   * - APB interface for communication with the system.
   * - Timer output signals, including count, maxReached, and PWM.
   * - Interrupt signals for timer events.
   * - A vector of clocks for clock selection.
   */
  val io = IO(new Bundle {
    /** APB interface for communication with the system. */
    val apb = new ApbBundle(ApbParams(timerParams.dataWidth, timerParams.addressWidth))
    /** Timer output signals, including count, maxReached, and PWM. */
    val timerOutput = new TimerOutputBundle(timerParams)
    /** Interrupt signals for timer events. */
    val interrupt = new TimerInterruptBundle
    /** A vector of clocks for clock selection. */
    val clocks = Vec(clockParams.numClocks, Clock())
  })

  // Derive ApbParams from timerParams
  val apbParams = ApbParams(timerParams.dataWidth, timerParams.addressWidth)

  /** Calculate the memory size for clock selection.
   *
   * The size is determined by the number of clocks, which requires `log2Ceil(numClocks)` bits.
   */
  val clockSelectSize = log2Ceil(clockParams.numClocks)

  // ###################
  // Bit Size Section
  // ###################
  /** Bit size of each input variable for the timer.
   *
   * This defines the number of bits required for each programmable input to the timer.
   */
  val enBitSize = 1                     // Enable signal (boolean)
  val prescalerBitSize = timerParams.countWidth // Prescaler value
  val maxCountBitSize = timerParams.countWidth  // Maximum count value
  val pwmCeilingBitSize = timerParams.countWidth // PWM ceiling value
  val setClockValueBitSize = timerParams.countWidth // Set clock value
  val setClockBitSize = 1               // Set clock signal (boolean)
  val clockSelectBitSize = clockSelectSize // Clock selection value

  // ###################
  // Memory Sizes Section
  // ###################
  /** Function to calculate the number of addresses required for a given bit size.
   *
   * This function calculates the number of APB data words required to store a value of a given bit size.
   *
   * @param bitSize The bit size of the value.
   * @param dataWidth The width of the APB data bus.
   * @return The number of addresses required to store the value.
   */
  def calculateAddressSize(bitSize: Int, dataWidth: Int): Int = {
    (bitSize + dataWidth - 1) / dataWidth
  }

  /** Calculate memory sizes for AddrDecode based on the number of programmable registers in the timer.
   *
   * The memory sizes are calculated as the number of APB data words required to store each timer parameter.
   */
  val memorySizes = Seq(
    enBitSize,          // en
    prescalerBitSize,   // prescaler
    maxCountBitSize,    // maxCount
    pwmCeilingBitSize,  // pwmCeiling
    setClockValueBitSize, // setClockValue
    setClockBitSize,    // setClock
    clockSelectBitSize  // clockSelect
  ).map(bitSize => calculateAddressSize(bitSize, apbParams.PDATA_WIDTH))

  // ###################
  // Address Range Section
  // ###################
  /** Address ranges for each input variable.
   *
   * This defines the address range (start and end) for each programmable input to the timer.
   * Each range corresponds to the addresses needed to store the input in the register space.
   */
  val enAddressRange = (0, memorySizes(0) - 1)                     // en
  val prescalerAddressRange = (enAddressRange._2 + 1, enAddressRange._2 + memorySizes(1)) // prescaler
  val maxCountAddressRange = (prescalerAddressRange._2 + 1, prescalerAddressRange._2 + memorySizes(2)) // maxCount
  val pwmCeilingAddressRange = (maxCountAddressRange._2 + 1, maxCountAddressRange._2 + memorySizes(3)) // pwmCeiling
  val setClockValueAddressRange = (pwmCeilingAddressRange._2 + 1, pwmCeilingAddressRange._2 + memorySizes(4)) // setClockValue
  val setClockAddressRange = (setClockValueAddressRange._2 + 1, setClockValueAddressRange._2 + memorySizes(5)) // setClock
  val clockSelectAddressRange = (setClockAddressRange._2 + 1, setClockAddressRange._2 + memorySizes(6)) // clockSelect

  /** Derive AddrDecodeParams from timerParams.
   *
   * This includes the data width, address width, and memory sizes for the address decoder.
   */
  val addrDecodeParams = AddrDecodeParams(
    dataWidth = timerParams.dataWidth,
    addressWidth = timerParams.addressWidth,
    memorySizes = memorySizes
  )

  /** Instantiate the vector of registers used to store the timer input values.
   *
   * This register space is used to store the programmable timer parameters, such as prescaler, maxCount, etc.
   * The size of the register space is determined by the sum of the memory sizes.
   */
  val totalAddresses = memorySizes.sum
  val timerInputRegisterSpace = RegInit(VecInit(Seq.fill(totalAddresses)(0.U(timerParams.countWidth.W))))

  /** Function to map values from the vector and mask off the end.
   *
   * This function extracts the value from the register space and masks it to the correct bit width.
   *
   * @param startAddress The starting address of the input in the register space.
   * @param bitSize The bit size of the input.
   * @return The extracted and masked value.
   */
  def mapVectorToValue(startAddress: Int, bitSize: Int): UInt = {
    val value = Wire(UInt(bitSize.W))
    value := 0.U
    for (i <- 0 until (bitSize + apbParams.PDATA_WIDTH - 1) / apbParams.PDATA_WIDTH) {
      value := value | (timerInputRegisterSpace(startAddress + i) << (i * apbParams.PDATA_WIDTH).U)
    }
    value(bitSize - 1, 0)
  }

  /** Instantiate the AddrDecode module.
   *
   * This module decodes the APB address into the appropriate register space.
   */
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := io.apb.PADDR
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  /** Instantiate the ApbInterface module.
   *
   * This module handles APB transactions and connects to the timerInputRegisterSpace for reads and writes.
   */
  val apbInterface = Module(new ApbInterface(apbParams))
  apbInterface.io.apb <> io.apb

  /** Connect the memory interface of ApbInterface to the timerInputRegisterSpace.
   *
   * The address output from the AddrDecode module is used to select the appropriate register.
   */
  apbInterface.io.mem.addr := addrDecode.io.addrOut
  apbInterface.io.mem.wdata := io.apb.PWDATA
  apbInterface.io.mem.read := !io.apb.PWRITE
  apbInterface.io.mem.write := io.apb.PWRITE

  /** Handle writes to the timerInputRegisterSpace.
   *
   * When a write operation is detected, the appropriate register in the timerInputRegisterSpace is updated.
   */
  when(apbInterface.io.mem.write) {
    for (i <- 0 until totalAddresses) {
      when(addrDecode.io.sel(i)) {
        timerInputRegisterSpace(i) := apbInterface.io.mem.wdata
      }
    }
  }

  /** Handle reads from the timerInputRegisterSpace.
   *
   * When a read operation is detected, the appropriate register in the timerInputRegisterSpace is read.
   */
  when(apbInterface.io.mem.read) {
    apbInterface.io.mem.rdata := 0.U
    for (i <- 0 until totalAddresses) {
      when(addrDecode.io.sel(i)) {
        apbInterface.io.mem.rdata := timerInputRegisterSpace(i)
      }
    }
  }

  /** Instantiate the TimerClocked module.
   *
   * This module handles the core timer functionality and is clocked by the selected clock.
   */
  val timerClocked = Module(new TimerClocked(timerParams, clockParams))
  timerClocked.io.clockBundle.clocks := io.clocks
  timerClocked.io.clockBundle.clockSel := mapVectorToValue(clockSelectAddressRange._1, clockSelectBitSize)

  /** Connect the timerInputRegisterSpace to the TimerClocked inputs.
   *
   * The programmable timer parameters are provided to the TimerClocked module.
   */
  timerClocked.io.timerBundle.timerInputBundle.en := mapVectorToValue(enAddressRange._1, enBitSize)(0)
  timerClocked.io.timerBundle.timerInputBundle.prescaler := mapVectorToValue(prescalerAddressRange._1, prescalerBitSize)
  timerClocked.io.timerBundle.timerInputBundle.maxCount := mapVectorToValue(maxCountAddressRange._1, maxCountBitSize)
  timerClocked.io.timerBundle.timerInputBundle.pwmCeiling := mapVectorToValue(pwmCeilingAddressRange._1, pwmCeilingBitSize)
  timerClocked.io.timerBundle.timerInputBundle.setClockValue := mapVectorToValue(setClockValueAddressRange._1, setClockValueBitSize)
  timerClocked.io.timerBundle.timerInputBundle.setClock := mapVectorToValue(setClockAddressRange._1, setClockBitSize)(0)

  /** Connect the TimerClocked outputs to the top-level outputs.
   *
   * The timer output signals, such as count, maxReached, and PWM, are connected to the top-level outputs.
   */
  io.timerOutput <> timerClocked.io.timerBundle.timerOutputBundle

  /** Handle interrupts.
   *
   * An interrupt is generated when the timer reaches its maximum count.
   */
  io.interrupt.interrupt := TimerInterruptEnum.None
  when(timerClocked.io.timerBundle.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }

  /** Handle APB error conditions.
   *
   * If the AddrDecode module detects an out-of-range address, the PSLVERR signal is asserted.
   */
  when(addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange) {
    apbInterface.io.apb.PSLVERR := true.B
  }.otherwise {
    apbInterface.io.apb.PSLVERR := false.B
  }
}