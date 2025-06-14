package sniper

def commandDelete(context: Context) =
  import context.*
  val indexed =
    db.getAll()
      .map(snip => s"${snip.id}: ${snip.description}" -> snip)
      .sortBy(_._2.id)
      .reverse

  val ids = prompts
    .multiChoiceNoneSelected(
      "Select a snippet you want to delete",
      indexed.map(_._1).toList
    )
    .getOrThrow

  if ids.nonEmpty && prompts
      .confirm(
        s"Are you sure you want to delete ${ids.length} snippets?"
      )
      .getOrThrow
  then
    indexed
      .filter(k => ids.contains(k._1))
      .map(_._2)
      .foreach: snip =>
        db.delete(snip.id)
        files.delete(snip.id)
    scribe.info(s"${ids.length} snippets deleted")
  end if

  Result.None
end commandDelete
