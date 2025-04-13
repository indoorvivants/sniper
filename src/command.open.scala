package sniper

def commandOpen(context: Context, cli: CLI.Open) =
  import context.*
  val all = db.getAll()

  val indexed =
    all
      .map(snip => s"${snip.id}: ${snip.description}" -> snip)
      .sortBy(_._2.id)
      .reverse

  def selectSnippet =
    val id = prompts
      .singleChoice(
        "Select a snippet you want to see",
        indexed.map(_._1).toList
      )
      .getOrThrow
    indexed.find(_._1 == id).map(_._2).get
  end selectSnippet

  val snippet = cli.id match
    case None => selectSnippet
    case Some(value) =>
      all
        .find(_.id == value)
        .getOrElse(sys.error(s"Snippet with id $value not found"))

  print(files.resolve(snippet.id))
end commandOpen
