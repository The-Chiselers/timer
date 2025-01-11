package tech.rocksavage.chiselware.timer.config

import tech.rocksavage.config.ConfigTrait
import tech.rocksavage.chiselware.timer.param.TimerParams

class TimerConfig extends ConfigTrait {
  override def getDefaultConfigs: Map[String, Any] = Map(
    "config1" -> TimerParams(dataWidth = 8, addressWidth = 8, countWidth = 8),
    "config2" -> TimerParams(dataWidth = 16, addressWidth = 16, countWidth = 16),
    "config3" -> TimerParams(dataWidth = 32, addressWidth = 32, countWidth = 32),
    "config4" -> TimerParams(dataWidth = 64, addressWidth = 64, countWidth = 64)
  )
}