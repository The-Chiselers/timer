// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.timer

/** Default parameter settings for the AddressDecoder
  *
  * @constructor
  *   default parameter settings
  * @param dataWidth
  *   specifies the width of the data bus
  * @param addressWidth
  *   specifies the width of the address bus
  * @param memorySizes
  *   specifies the size of each memory range
  * @author
  *   Warren Savage
  * @version 1.0
  *
  * @see
  *   [[http://www.rocksavage.tech]] for more information
  */
case class TimerParams(
    dataWidth: Int = 8,
    addressWidth: Int = 8,

    countWidth: Int = 8,
) {

  require(dataWidth >= 1, "Data Width must be greater than or equal 1")
  require(addressWidth >= 1, "Address Width must be greater than or equal 1")

}
