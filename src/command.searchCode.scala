package sniper

import trigs.InteractiveProgress

def commandSearchCode(ctx: Context, cli: CLI.SearchCode) =
  var query = cli.query.getOrElse(ctx.prompts.text("Query").getOrThrow)
  var results = searchResults(ctx, query).toArray
  var commandResult = Option.empty[Result]
  while commandResult.isEmpty do
    if results.isEmpty then
      ctx.clearTerminal()
      System.err.println(
        Console.BOLD + Console.RED + "No results" + Console.RESET
      )
      query = ctx.prompts.text("Query").getOrThrow
      if query == "" then commandResult = Some(Result.None)
      else results = searchResults(ctx, query).toArray
    else
      val choice = ctx.prompts
        .text(
          s"Which result do you want to see? [1-${results.size}, 's' for new search, q for quit]"
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
            val sr = results(value - 1)
            commandResult = Some(
              Result.Open(ctx.files.resolve(sr.snippetId) / sr.path)
            )
          case _ => sys.error("invalid choice")

      end if
  end while

  commandResult.getOrElse(Result.None)

end commandSearchCode

case class SearchResults(
    line: String,
    snippetTitle: String,
    path: os.RelPath,
    snippetId: Long
)

def printResults(res: List[SearchResults]) =
  var prevSnippetId = -1L
  val resultsCount = res.size
  val formatNum =
    val targetLength = resultsCount.toString.length
    (s: Int) => s.toString.reverse.padTo(targetLength, ' ').reverse
  res.zipWithIndex.foreach: (sr, idx) =>
    if sr.snippetId != prevSnippetId then
      System.err.println(Console.BOLD + sr.snippetTitle + Console.RESET)
      prevSnippetId = sr.snippetId

    System.err.println(s" ${formatNum(idx + 1)}  | " + sr.line)
end printResults

def searchResults(ctx: Context, query: String) =
  val results = ctx.codesearch.search(query)
  lazy val caches = collection.mutable.Map.empty[os.Path, Array[String]]
  lazy val meta = collection.mutable.Map.empty[Long, Snippet]
  val base = ctx.config.data
  val resolved = List.newBuilder[SearchResults]

  results.zipWithIndex.foreach:
    case ((rp, snippetId, line), idx) =>
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

      resolved += SearchResults(
        lines(line - 1),
        snippet.description,
        rp,
        snippetId
      )

  resolved.result
end searchResults
