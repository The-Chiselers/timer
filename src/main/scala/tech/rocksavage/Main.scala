package tech.rocksavage

import chisel3._

import java.io.File
import tech.rocksavage.args.Conf
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.synth.Synth.{genVerilogFromModuleName, synthesizeFromModuleName}

import scala.sys.exit

object Main {
  def main(args_array: Array[String]): Unit = {
    val conf = new Conf(args_array.toIndexedSeq)

    // Define multiple default configurations
    val defaultConfigs = Map(
      "config1" -> TimerParams(dataWidth = 32, addressWidth = 32, countWidth = 32),
      "config2" -> TimerParams(dataWidth = 16, addressWidth = 16, countWidth = 16),
      "config3" -> TimerParams(dataWidth = 64, addressWidth = 64, countWidth = 64),
      "config4" -> TimerParams(dataWidth = 8, addressWidth = 8, countWidth = 8)
    )

    val build_folder = new File("out")

    conf.subcommand match {
      case Some(conf.verilog) => {
        // Run Verilog generation for each configuration
        defaultConfigs.foreach { case (name, params) =>
          println(s"Generating Verilog for configuration: $name")
          val verilogString = genVerilogFromModuleName(conf.verilog.module(), params)
          conf.verilog.mode() match {
            case "print" => {
              println(verilogString)
            }
            case "write" => {
              val filename = s"${conf.verilog.module()}_$name.sv"
              val f = new File(filename)
              val bw = new java.io.BufferedWriter(new java.io.FileWriter(f))
              bw.write(verilogString)
              bw.close()
            }
          }
        }
      }
      case Some(conf.synth) => {
        // Run synthesis for each configuration
        defaultConfigs.foreach { case (name, params) =>
          println(s"Synthesizing configuration: $name")
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
          val synth = synthesizeFromModuleName(synthConfig, conf.synth.module(), params)
          println(synth.getStdout)
          println(synth.getSynthString)

          // mkdir $build_folder/synth/$name
          val synth_folder = new File(s"$build_folder/synth/$name")
          synth_folder.mkdirs()

          // write $build_folder/synth/$name/$module_net.v
          val net_file = new File(s"$build_folder/synth/$name/${conf.synth.module()}_net.v")
          net_file.createNewFile()
          val net_bw = new java.io.BufferedWriter(new java.io.FileWriter(net_file))
          net_bw.write(synth.getSynthString)

          // write $build_folder/synth/$name/log.txt
          val log_file = new File(s"$build_folder/synth/$name/log.txt")
          log_file.createNewFile()
          val log_bw = new java.io.BufferedWriter(new java.io.FileWriter(log_file))
          log_bw.write(synth.getStdout)

          // write $build_folder/synth/$name/gates.txt
          val gates_file = new File(s"$build_folder/synth/$name/gates.txt")
          gates_file.createNewFile()
          val gates_bw = new java.io.BufferedWriter(new java.io.FileWriter(gates_file))
          gates_bw.write(synth.getGates)

        }
      }
      case _ => {
        println("No subcommand given")
      }
    }
  }
}
