// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer.bundle

import chisel3._

class TimerInterruptBundle extends Bundle {
    val maxCountInterrupt: Bool = Output(Bool())
}
