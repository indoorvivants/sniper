package sniper

import trigs.InteractiveProgress

def commandSearchCode(ctx: Context, cli: CLI.SearchCode) =
  var query = cli.query.getOrElse(ctx.prompts.text("Query").getOrThrow)
  var results = searchResults(ctx, query)
  var stop = false
  while !stop do
    if results.isEmpty then
      ctx.clearTerminal()
      System.err.println(
        Console.BOLD + Console.RED + "No results" + Console.RESET
      )
      query = ctx.prompts.text("Query").getOrThrow
      if query == "" then stop = true
      else results = searchResults(ctx, query)
    else
      val choice = ctx.prompts
        .text(
          s"Which result do you want to see? [1-${results.size}, 's' for new search, q for quit]"
        )
        .getOrThrow
        .trim

      if choice == "q" then stop = true
      else if choice == "s" then
        ctx.clearTerminal()
        query = ctx.prompts.text("Query").getOrThrow
        results = searchResults(ctx, query)
      else
        choice.toIntOption match
          case Some(value) if value >= 1 && value <= results.size =>
            val (filePath, snippetId, line) = results(value - 1)
            print(ctx.files.resolve(snippetId) / filePath)
            stop = true
          case _ => sys.error("invalid choice")

      end if
  end while

end commandSearchCode

def searchResults(ctx: Context, query: String) =
  val results = ctx.codesearch.search(query)
  lazy val caches = collection.mutable.Map.empty[os.Path, Array[String]]
  lazy val meta = collection.mutable.Map.empty[Long, Snippet]
  val base = ctx.config.data
  var prevSnippetId = -1L
  val resultsCount = results.size
  val formatNum =
    val targetLength = resultsCount.toString.length
    (s: Int) => s.toString.reverse.padTo(targetLength, ' ').reverse
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

      if snippetId != prevSnippetId then
        System.err.println(Console.BOLD + snippet.description + Console.RESET)
        prevSnippetId = snippetId

      System.err.println(s" ${formatNum(idx + 1)}  | " + lines(line - 1))
  results
end searchResults
