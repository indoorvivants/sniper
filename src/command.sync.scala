package sniper

import cue4s.*

def commandSync(ctx: Context) =
  given trigs.Progress =
    trigs.InteractiveProgress(
      cue4s.AnsiTerminal(cue4s.Output.Std),
      cue4s.Output.Std
    )

  ctx.codesearch.withUpdater: updater =>
    ctx.db
      .getAll()
      .foreach: snip =>
        val base = ctx.files.resolve(snip.id)
        val files = ctx.db
          .getFiles(snip.id)
          .map(_.filename)
          .map(os.RelPath(_))
          .map(base / _)

        updater(files)
end commandSync
