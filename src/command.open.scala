package sniper

def commandOpen(context: Context) =
  import context.*
  val all = db.getAll()

  val indexed =
    all
      .map(snip => s"${snip.id}: ${snip.description}" -> snip)
      .sortBy(_._2.id)
      .reverse

  val id = prompts
    .singleChoice(
      "Select a snippet you want to see",
      indexed.map(_._1).toList
    )
    .getOrThrow

  val snippet = indexed.find(_._1 == id).map(_._2).get

  println(files.resolve(snippet.id))
end commandOpen
