package sniper

import cue4s.*

def commandSync(ctx: Context) =
  given trigs.Progress = trigs.Progress.Quiet

  val snippetsToDelete = List.newBuilder[Long]
  val snippetFilesToRemove = List.newBuilder[SnippetFile]

  ctx.codesearch
    .withUpdater(empty = true): updater =>
      ctx.db
        .getAll()
        .foreach: snip =>
          val base = ctx.files.resolve(snip.id)
          val allSnippetFiles = ctx.db.getFiles(snip.id)
          val filesToDeregister =
            allSnippetFiles
              .filter: f =>
                val path = base / os.RelPath(f.filename)
                !os.exists(path)
              .toSet

          snippetFilesToRemove ++= filesToDeregister

          val filesToReindex = allSnippetFiles.filterNot(
            filesToDeregister.contains
          )

          if filesToDeregister.nonEmpty then
            scribe.warn(
              s"Snippet ${snip.id} has files that no longer exist, they will be removed from database: ${filesToDeregister
                  .map(_.filename)}"
            )
          end if

          def withBase(snips: Iterable[SnippetFile]) =
            snips.map(f => base / os.RelPath(f.filename))

          if !os.exists(base) then
            scribe.warn(
              s"Snippet ${snip.id} (${snip.description}) no longer exists, codesearch index will be deleted"
            )
            snippetsToDelete += snip.id
            updater(UpdateAction.Delete, withBase(allSnippetFiles).toVector)
          else if filesToReindex.nonEmpty then
            scribe.info(
              s"Reindexing snippet ${snip.id} (${snip.description}): ${filesToReindex.map(_.filename)}"
            )
            updater(UpdateAction.Reindex, withBase(filesToReindex).toVector)
          else
            scribe.warn(
              s"Snippet ${snip.id} (${snip.description}) has no files to reindex"
            )
          end if

  snippetsToDelete
    .result()
    .foreach: id =>
      scribe.info(s"Deleting snippet $id from database")
      ctx.db.delete(id)

  Result.None
end commandSync
