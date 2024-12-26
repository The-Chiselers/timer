//package tech.rocksavage.chiselware.Timer
//
//import chisel3._
//import chisel3.util._
//
//class TimerRegs(p: BaseParams) extends Bundle {
//
//  // Internal Register Sizes
//
//  val TIMER_SIZE: Int      = p.timer0
//  val IRQ_ENABLE_SIZE: Int = 1
//
//  // #####################################################################
//  // REGS
//  // #####################################################################
//  // Common Registers
//
//  val COUNT_SIZE    = TIMER_SIZE
//  val VALUE_SIZE    = TIMER_SIZE
//  val COMPARE_SIZE  = TIMER_SIZE
//  val ENABLE_SIZE   = 1
//  val MODE_SIZE     = 1
//  val PRESCALE_SIZE = 3
//
//  // ---
//
//  val COUNT_NUM_REGS: Int =
//    (COUNT_SIZE + p.dataWidth - 1) / p.dataWidth
//  val VALUE_NUM_REGS: Int =
//    (VALUE_SIZE + p.dataWidth - 1) / p.dataWidth
//  val COMPARE_NUM_REGS: Int =
//    (COMPARE_SIZE + p.dataWidth - 1) / p.dataWidth
//
//  val COUNT = RegInit(
//    VecInit(Seq.fill(COUNT_NUM_REGS)(0.U(p.dataWidth.W)))
//  )
//  val VALUE = RegInit(
//    VecInit(Seq.fill(VALUE_NUM_REGS)(0.U(p.dataWidth.W)))
//  )
//  val COMPARE = RegInit(
//    VecInit(Seq.fill(COMPARE_NUM_REGS)(0.U(p.dataWidth.W)))
//  )
//  val ENABLE = RegInit(
//    VecInit(Seq.fill(ENABLE_SIZE)(0.U(p.dataWidth.W)))
//  )
//  val MODE = RegInit(
//    VecInit(Seq.fill(MODE_SIZE)(0.U(p.dataWidth.W)))
//  )
//  val PRESCALE = RegInit(
//    VecInit(Seq.fill(PRESCALE_SIZE)(0.U(p.dataWidth.W)))
//  )
//
//  val IRQ_ENABLE: Int = p.dataWidth
//
//  // #####################################################################
//
//  val COUNT_ADDR: Int     = 0
//  val COUNT_REG_SIZE: Int = (TIMER_SIZE)
//  val COUNT_ADDR_MAX: Int = COUNT_ADDR + COUNT_REG_SIZE - 1
//
//  val VALUE_ADDR: Int     = COUNT_ADDR + 1
//  val VALUE_REG_SIZE: Int = (TIMER_SIZE)
//  val VALUE_ADDR_MAX: Int = VALUE_ADDR + VALUE_REG_SIZE - 1
//
//  val COMPARE_ADDR: Int     = VALUE_ADDR + 1
//  val COMPARE_REG_SIZE: Int = (TIMER_SIZE)
//  val COMPARE_ADDR_MAX: Int = COMPARE_ADDR + COMPARE_REG_SIZE - 1
//
//  val ENABLE_ADDR: Int     = COMPARE_ADDR + 1
//  val ENABLE_REG_SIZE: Int = (TIMER_SIZE)
//  val ENABLE_ADDR_MAX: Int = ENABLE_ADDR + ENABLE_REG_SIZE - 1
//
//  val MODE_ADDR: Int     = ENABLE_ADDR + 1
//  val MODE_REG_SIZE: Int = (TIMER_SIZE)
//  val MODE_ADDR_MAX: Int = MODE_ADDR + MODE_REG_SIZE - 1
//
//  val PRESCALE_ADDR: Int     = MODE_ADDR + 1
//  val PRESCALE_REG_SIZE: Int = (TIMER_SIZE)
//  val PRESCALE_ADDR_MAX: Int = PRESCALE_ADDR + PRESCALE_REG_SIZE - 1
//
//  // val IRQ_ENABLE_ADDR: Int     = TRIGGER_STATUS_ADDR_MAX + 1
//  // val IRQ_ENABLE_REG_SIZE: Int = 1
//  // val IRQ_ENABLE_ADDR_MAX: Int = IRQ_ENABLE_ADDR + IRQ_ENABLE_REG_SIZE - 1
//
//  // #####################################################################
//  def readCount(): UInt = {
//    val temp = Cat(COUNT.reverse)
//    temp
//  }
//  def writeCount(data: UInt): Unit = {
//    COUNT := data.asTypeOf(COUNT).reverse
//  }
//
//  def readValue(): UInt = {
//    val temp = Cat(VALUE.reverse)
//    temp
//  }
//  def writeValue(data: UInt): Unit = {
//    VALUE := data.asTypeOf(VALUE).reverse
//  }
//
//  def readCompare(): UInt = {
//    val temp = Cat(COMPARE.reverse)
//    temp
//  }
//  def writeCompare(data: UInt): Unit = {
//    COMPARE := data.asTypeOf(COMPARE).reverse
//  }
//
//  def readEnable(): UInt = {
//    val temp = Cat(ENABLE.reverse)
//    temp
//  }
//  def writeEnable(data: UInt): Unit = {
//    ENABLE := data.asTypeOf(ENABLE).reverse
//  }
//
//  def readMode(): UInt = {
//    val temp = Cat(MODE.reverse)
//    temp
//  }
//  def writeMode(data: UInt): Unit = {
//    MODE := data.asTypeOf(MODE).reverse
//  }
//
//  def readPrescale(): UInt = {
//    val temp = Cat(PRESCALE.reverse)
//    temp
//  }
//  def writePrescale(data: UInt): Unit = {
//    PRESCALE := data.asTypeOf(PRESCALE).reverse
//  }
//
//}
