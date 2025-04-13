package sniper

import cue4s.*

def commandSync(ctx: Context) =
  given trigs.Progress = trigs.Progress.Quiet

  val snippetsToDelete = List.newBuilder[Long]

  ctx.codesearch.withUpdater(empty = true): updater =>
    ctx.db
      .getAll()
      .foreach: snip =>
        val base = ctx.files.resolve(snip.id)
        val files = ctx.db
          .getFiles(snip.id)
          .map(f => os.RelPath(f.filename))
          .map(base / _)
        if !os.exists(base) then
          scribe.warn(
            s"Snippet ${snip.id} (${snip.description}) no longer exists, codesearch index will be deleted"
          )
          snippetsToDelete += snip.id
          updater(UpdateAction.Delete, files)
        else updater(UpdateAction.Reindex, files)
        end if
  snippetsToDelete
    .result()
    .foreach: id =>
      scribe.info(s"Deleting snippet $id")
      ctx.db.delete(id)
end commandSync
