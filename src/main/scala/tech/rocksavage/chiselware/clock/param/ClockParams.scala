// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.clock.param

case class ClockParams(
  // Number of to pass in
  numClocks: Int = 1
) {

  require(numClocks >= 1, "Number of Clocks must be greater than or equal 1")
}
