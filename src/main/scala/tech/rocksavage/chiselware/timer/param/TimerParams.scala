// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.timer.param

/** Default parameter settings for the Timer
  *
  * @constructor
  *   default parameter settings
  * @param dataWidth
  *   specifies the width of the data bus
  * @param addressWidth
  *   specifies the width of the address bus
  * @param countWidth
  *   specifies the size of the counter
  * @author
  *   Warren Savage
  * @version 1.0
  *
  * @see
  *   [[http://www.rocksavage.tech]] for more information
  */

case class TimerParams(
    // Parameters for addressing
    dataWidth: Int = 32,
    addressWidth: Int = 32,
    wordWidth: Int = 8,

    // Parameters for the counter
    countWidth: Int = 32,
    prescalerWidth: Int = 32,

    // Cov
    coverage: Boolean = false,
    verbose: Boolean = false) {

    require(dataWidth >= 1, "Data Width must be greater than or equal 1")
    require(addressWidth >= 1, "Address Width must be greater than or equal 1")
    require(countWidth >= 1, "Count Width must be greater than or equal 1")
    require(
      prescalerWidth >= 1,
      "Prescaler Width must be greater than or equal 1",
    )
}
