package sniper

import trigs.InteractiveProgress

def commandSearchCode(ctx: Context, cli: CLI.SearchCode) =
  var query = cli.query.getOrElse(ctx.prompts.text("Query").getOrThrow)
  var results = searchResults(ctx, query).toArray
  var commandResult = Option.empty[Result]
  while commandResult.isEmpty do
    if results.isEmpty then
      ctx.clearTerminal()
      System.err
        .println(Console.BOLD + Console.RED + "No results" + Console.RESET)
      query = ctx.prompts.text("Query").getOrThrow
      if query == "" then commandResult = Some(Result.None)
      else results = searchResults(ctx, query).toArray
    else
      val mapping = printResults(results)
      val choice =
        ctx.prompts
          .text(
            s"Which result do you want to see? [1-${mapping.size}, 's' for new search, q for quit]"
          )
          .getOrThrow
          .trim

      if choice == "q" then commandResult = Some(Result.None)
      else if choice == "s" then
        ctx.clearTerminal()
        query = ctx.prompts.text("Query").getOrThrow
        results = searchResults(ctx, query).toArray
      else
        choice.toIntOption match
          case Some(value) if value >= 1 && value <= results.size =>
            val sr = mapping(value)
            commandResult = Some(
              Result.Open(ctx.files.resolve(sr.snippetId) / sr.path)
            )
          case _ =>
            sys.error("invalid choice")

      end if
  end while

  commandResult.getOrElse(Result.None)

end commandSearchCode

case class LineMatch(
    lineNumber: Int,
    line: String,
    snippetTitle: String,
    path: os.RelPath,
    snippetId: Long,
    score: Float
) derives upickle.default.ReadWriter

given upickle.default.ReadWriter[os.RelPath] = upickle.default
  .readwriter[String]
  .bimap(_.toString, os.RelPath(_))

case class Loc(line: String, lineNumber: Int, path: os.RelPath, score: Float)
    derives upickle.default.ReadWriter

case class SnippetResult(
    snippetId: Long,
    snippetTitle: String,
    locations: Seq[Loc],
    score: Float
) derives upickle.default.ReadWriter

def groupResults(res: Array[LineMatch]): Array[SnippetResult] =
  res
    .groupBy(_.snippetId)
    .map: (snippetId, results) =>
      val topScores = results.sortBy(_.score * -1).take(5)
      val snippetScore = topScores.map(_.score).sum

      SnippetResult(
        snippetId,
        results.head.snippetTitle,
        locations =
          topScores.map(sr => Loc(sr.line, sr.lineNumber, sr.path, sr.score)),
        snippetScore
      )
    .toArray
    .sortBy(_.score * -1)

def printResults(grouped: Array[SnippetResult]) =
  val formatNum =
    val targetLength = grouped.map(_.locations.length).sum.toString.length
    (s: Int) => s.toString.reverse.padTo(targetLength, ' ').reverse

  var idx = 0
  val mapping = Map.newBuilder[
    Int,
    (snippetId: Long, line: String, lineNumber: Int, path: os.RelPath)
  ]

  grouped.foreach: sr =>
    System.err.println(
      Console.BOLD + Console.YELLOW + sr.snippetTitle + Console.RESET + s" (${sr.snippetId})"
    )
    sr.locations.foreach:
      case Loc(line, lineNumber, path, score) =>
        idx += 1
        System.err.println(
          s" ${formatNum(idx)}  | " + Console.BOLD + path + ":" + lineNumber + "  " + Console.RESET + line.trim
        )
        mapping += idx -> (sr.snippetId, line, lineNumber, path)

  mapping.result()
end printResults

def searchResults(
    ctx: Context,
    query: String,
    limit: Int = 5
): Array[SnippetResult] =
  val results = ctx.codesearch.search(query)
  lazy val caches = collection.mutable.Map.empty[os.Path, Array[String]]
  lazy val meta = collection.mutable.Map.empty[Long, Snippet]
  val base = ctx.config.data
  val resolved = Array.newBuilder[LineMatch]

  results.zipWithIndex
    .foreach:
      case ((rp, snippetId, line, score), idx) =>
        val snippetRoot = base / snippetId.toString

        val lines = caches.getOrElseUpdate(
          snippetRoot / rp,
          os.read.lines(snippetRoot / rp).toArray
        )

        val snippet = meta.getOrElseUpdate(
          snippetId,
          ctx.db
            .get(snippetId)
            .getOrElse(sys.error(s"Snippet $snippetId not found"))
        )

        resolved +=
          LineMatch(
            line,
            lines(line - 1),
            snippet.description,
            rp,
            snippetId,
            score
          )

  resolved.result()

  groupResults(resolved.result()).take(limit)
end searchResults
