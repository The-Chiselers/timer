package tech.rocksavage

import chisel3._

import java.io.File
import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.{genVerilogFromModuleName, synthesizeFromModuleName}

import scala.sys.exit

object Main {
  def main(args_array: Array[String]): Unit = {
    val conf = new Conf(args_array.toIndexedSeq)
    conf.subcommand match {
      case Some(conf.verilog) => {
        val verilogString = genVerilogFromModuleName(conf.verilog.module())
        conf.verilog.mode() match {
          case "print" => {
            println(verilogString)
          }
          case "write" => {
            val filename = conf.verilog.module() + ".sv"
            // write to file
            def f = new File(filename)
            val bw = new java.io.BufferedWriter(new java.io.FileWriter(f))
            bw.write(verilogString)
          }
        }
      }
      case Some(conf.synth) => {
        val synthCommands = List(
          tech.rocksavage.synth.SynthCommand.Synth,
          tech.rocksavage.synth.SynthCommand.Flatten,
          tech.rocksavage.synth.SynthCommand.Dfflibmap,
          tech.rocksavage.synth.SynthCommand.Abc,
          tech.rocksavage.synth.SynthCommand.Opt,
          tech.rocksavage.synth.SynthCommand.Clean,
          tech.rocksavage.synth.SynthCommand.Stat
        )
        val synthConfig = new tech.rocksavage.synth.SynthConfig(
          conf.synth.techlib(),
          synthCommands
        )
        val synth = synthesizeFromModuleName(synthConfig, conf.synth.module())
        println(synth.getStdout)
        println(synth.getSynthString)
      }
      case _ => {
        println("No subcommand given")
      }
    }
  }
}

