package tech.rocksavage.args

import org.rogach.scallop.{ScallopConf, Subcommand}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  object verilog extends Subcommand("verilog") {
    val mode = opt[String](default = Some("print"), validate = List("print", "write").contains(_))
    val module = opt[String](required = true)
  }

  object synth extends Subcommand("synth") {
    val module = opt[String](required = true)
    var techlib = opt[String](required = true)
  }

  addSubcommand(verilog)
  addSubcommand(synth)
  verify()
}
