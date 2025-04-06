package sniper

import trigs.InteractiveProgress

def commandSearchCode(ctx: Context, cli: CLI.SearchCode) =
  val query = cli.query.getOrElse(ctx.prompts.text("Query").getOrThrow)
  val results = ctx.codesearch.search(query)
  lazy val caches = collection.mutable.Map.empty[os.Path, Array[String]]
  lazy val meta = collection.mutable.Map.empty[Long, Snippet]
  val base = ctx.config.data
  var prevSnippetId = -1L
  results.foreach: (rp, snippetId, line) =>
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
      println(Console.BOLD + snippet.description + Console.RESET)
      prevSnippetId = snippetId

    println("   | " + lines(line - 1))

end commandSearchCode
