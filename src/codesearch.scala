package sniper

import trigs.*
import os.Path

class CodeSearch private (config: CodeSearch.Config, searchable: Searchable):
  def pathResolver(path: os.Path): (os.RelPath, Int) =
    val rel = path.relativeTo(config.codeDir)
    val first = rel.segments.head.toInt
    (path.relativeTo(config.codeDir / first.toString), first)

  def search(query: String): List[(os.RelPath, Int, Int)] =
    searchable
      .search(query)
      .map: (loc, _) =>
        pathResolver(loc.file) :* loc.line
  end search

  def withUpdater(f: (Seq[os.Path] => Unit) => Unit)(using
      Progress
  ): CodeSearch =
    val toUpdate = List.newBuilder[os.Path]
    f(toUpdate.addAll(_))
    val relativePaths = toUpdate.result()
    val indexer = TrigramIndexer()
    if relativePaths.nonEmpty then
      val builder = CodeSearch.load(config.indexerLocation)
      relativePaths.foreach: absPath =>
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
