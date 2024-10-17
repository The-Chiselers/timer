package tech.rocksavage.chiselware.GPIO

import chisel3._
import chisel3.util._

class TimerRegs(p: BaseParams) extends Bundle {

  // Internal Register Sizes
  val TIMER_SIZE: Int      = p.timer0
  val IRQ_ENABLE_SIZE: Int = 1

  // #####################################################################
  // REGS
  // #####################################################################
  // Common Registers

  val COUNT    = RegInit(0.U(TIMER_SIZE.W))
  val VALUE    = RegInit(0.U(TIMER_SIZE.W))
  val COMPARE  = RegInit(0.U(TIMER_SIZE.W))
  val ENABLE   = RegInit(0.U(1.W))
  val MODE     = RegInit(0.U(1.W))
  val PRESCALE = RegInit(0.U(3.W))

  val IRQ_ENABLE: Int = p.dataWidth

  // #####################################################################

  val COUNT_ADDR: Int     = 0
  val COUNT_REG_SIZE: Int = (TIMER_SIZE)
  val COUNT_ADDR_MAX: Int = COUNT_ADDR + COUNT_REG_SIZE - 1

  val VALUE_ADDR: Int     = COUNT_ADDR + 1
  val VALUE_REG_SIZE: Int = (TIMER_SIZE)
  val VALUE_ADDR_MAX: Int = VALUE_ADDR + VALUE_REG_SIZE - 1

  val COMPARE_ADDR: Int     = VALUE_ADDR + 1
  val COMPARE_REG_SIZE: Int = (TIMER_SIZE)
  val COMPARE_ADDR_MAX: Int = COMPARE_ADDR + COMPARE_REG_SIZE - 1

  val ENABLE_ADDR: Int     = COMPARE_ADDR + 1
  val ENABLE_REG_SIZE: Int = (TIMER_SIZE)
  val ENABLE_ADDR_MAX: Int = ENABLE_ADDR + ENABLE_REG_SIZE - 1

  val MODE_ADDR: Int     = ENABLE_ADDR + 1
  val MODE_REG_SIZE: Int = (TIMER_SIZE)
  val MODE_ADDR_MAX: Int = MODE_ADDR + MODE_REG_SIZE - 1

  val PRESCALE_ADDR: Int     = MODE_ADDR + 1
  val PRESCALE_REG_SIZE: Int = (TIMER_SIZE)
  val PRESCALE_ADDR_MAX: Int = PRESCALE_ADDR + PRESCALE_REG_SIZE - 1

  val IRQ_ENABLE_ADDR: Int     = TRIGGER_STATUS_ADDR_MAX + 1
  val IRQ_ENABLE_REG_SIZE: Int = 1
  val IRQ_ENABLE_ADDR_MAX: Int = IRQ_ENABLE_ADDR + IRQ_ENABLE_REG_SIZE - 1

}
