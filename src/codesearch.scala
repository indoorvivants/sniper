package sniper

import trigs.*
import os.Path

enum UpdateAction:
  case Delete, Reindex

class CodeSearch private (config: CodeSearch.Config, searchable: Searchable):
  def pathResolver(path: os.Path): (path: os.RelPath, snippetId: Int) =
    val rel = path.relativeTo(config.codeDir)
    val first = rel.segments.head.toInt
    (path.relativeTo(config.codeDir / first.toString), first)

  def search(query: String) =
    searchable
      .search(query)
      .map: (loc, _) =>
        pathResolver(loc.file) ++ (line = loc.line)
  end search

  /** Use this function to update/delete paths in codesearch index. After `f`
    * finishes, index is written to the default location automatically.
    *
    * @param empty
    *   when true, don't load the existing codesearch index, start with an empty
    *   one instead
    * @param f
    * @return
    */
  def withUpdater(empty: Boolean)(
      f: ((action: UpdateAction, paths: Seq[os.Path]) => Unit) => Unit
  )(using
      Progress
  ): CodeSearch =
    val toIndex = List.newBuilder[os.Path]
    val toDelete = List.newBuilder[os.Path]
    f((action, paths) =>
      action match
        case UpdateAction.Delete  => toDelete ++= paths
        case UpdateAction.Reindex => toIndex ++= paths
    )
    val pathsToIndex = toIndex.result()
    val pathsToDelete = toDelete.result()
    val indexer = TrigramIndexer()
    if pathsToIndex.nonEmpty || pathsToDelete.nonEmpty then
      val builder =
        if empty then TrigramIndexMutableBuilder.empty
        else CodeSearch.load(config.indexerLocation)
      pathsToDelete.foreach: absPath =>
        builder.deleteFile(absPath)
      pathsToIndex.foreach: absPath =>
        val entry = indexer.indexFile(absPath)
        builder.deleteFile(absPath)
        entry.foreach(builder.addEntry(_))

      os.write.over(
        config.indexerLocation,
        TrigramIndexSerialiser.serialise(builder.toIndex)
      )

      new CodeSearch(config, builder.toSearchable)
    else this

    end if

  end withUpdater

end CodeSearch

object CodeSearch:
  case class Config(
      indexerLocation: os.Path,
      codeDir: os.Path
  )
  def apply(config: CodeSearch.Config): CodeSearch =
    new CodeSearch(config, load(config.indexerLocation).toSearchable)
  end apply

  private def load(location: os.Path) =
    if !os.exists(location) then
      scribe.warn(
        s"Codesearch index does not exist at ${location}, bootstrapping it as empty"
      )
      val newValue = TrigramIndexSerialiser.serialise(
        TrigramIndexMutableBuilder.empty.toIndex
      )(using Progress.Quiet)

      println(newValue.toList)
      os.write(
        location,
        newValue
      )
    end if

    TrigramIndexDeserialiser
      .deserialiseIntoBuilder(
        location.getInputStream
      )(using Progress.Quiet)
  end load

end CodeSearch
