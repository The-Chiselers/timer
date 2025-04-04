package tech.rocksavage.chiselware.timer

import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.traits.ModuleConfig

class TimerConfig extends ModuleConfig {
    override def getDefaultConfigs: Map[String, Any] = Map(
      "8_8_8" -> Seq(
        TimerParams(
          dataWidth = 8,
          addressWidth = 8,
          wordWidth = 8,
          countWidth = 8,
          prescalerWidth = 8
        ),
        false
      ),
      "16_16_16" -> Seq(
        TimerParams(
          dataWidth = 16,
          addressWidth = 16,
          wordWidth = 8,
          countWidth = 16,
          prescalerWidth = 16
        ),
        false
      ),
      "32_32_32" -> Seq(
        TimerParams(
          dataWidth = 32,
          addressWidth = 32,
          wordWidth = 8,
          countWidth = 32,
          prescalerWidth = 32
        ),
        false
      ),
      "64_64_64" -> Seq(
        TimerParams(
          dataWidth = 64,
          addressWidth = 64,
          wordWidth = 8,
          countWidth = 64,
          prescalerWidth = 64
        ),
        false
      )
    )
}
