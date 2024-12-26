////> using scala "2.13.12"
////> using dep "org.chipsalliance::chisel:6.5.0"
////> using plugin "org.chipsalliance:::chisel-plugin:6.5.0"
////> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ymacro-annotations"
//package tech.rocksavage.chiselware.timer
//
//import _root_.circt.stage.ChiselStage
//import _root_.circt.stage.FirtoolOption
//// third-party imports
//import chisel3._
//
//object Main extends App {
//
//  // ######### Getting Setup #########
//  // get build root, if not set use null
//  var output = sys.env.get("BUILD_ROOT")
//  if (output == null) {
//    println("BUILD_ROOT not set, please set and run again")
//    System.exit(1)
//  }
//  // set output directory
//  val outputUnwrapped = output.get
//  val outputDir       = s"$outputUnwrapped/verilog"
//
//  // firtool options for generating verilog
//  // val firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
//  val myParams =
//    BaseParams(dataWidth = 32, addrWidth = 32, coverage = false)
//  // if output dir does not exist, make path
//  val javaOutputDir = new java.io.File(outputDir)
//  if (!javaOutputDir.exists) javaOutputDir.mkdirs
//
//  // ######### Set Up Top Module HERE #########
//  val top_name = "Timer.sv"
//  ChiselStage.emitSystemVerilog(
//    new Timer(myParams),
//    firtoolOpts = Array(
//      "--lowering-options=disallowLocalVariables,disallowPackedArrays",
//      "--disable-all-randomization",
//      "--strip-debug-info",
//      "--split-verilog",
//      s"-o=$outputDir/"
//    )
//  )
//  // ##########################################
//  System.exit(0)
//}
