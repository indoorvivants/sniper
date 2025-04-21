package sniper

def commandPrintConfig(ctx: Context, cli: CLI.PrintConfig) =
  if cli.location then Result.Out(ctx.defaultLocations.configFile.toString)
  else
    scribe.info(s"Config from [${ctx.defaultLocations.configFile}]")
    Result.Out(pprint(ctx.config).toString)
