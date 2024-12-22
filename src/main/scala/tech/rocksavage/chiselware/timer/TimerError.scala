// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.timer

import chisel3._

object TimerError extends ChiselEnum {
  val None, AddressOutOfRange = Value
}
