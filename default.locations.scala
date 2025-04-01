package sniper

class DefaultLocations(home: os.Path):
  private val NAME = "sniper"
  lazy val configDir = home / ".config" / NAME
  lazy val configFile = configDir / "config.toml"
  lazy val share = home / ".local" / "share" / NAME
  lazy val state = home / ".local" / "state" / NAME
end DefaultLocations
