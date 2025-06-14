package sniper

def commandNew(context: Context, cli: CLI.New): Result.Open =
  import context.*
  val attrs =
    val description = cli.description.getOrElse(promptDescription(prompts))

    SnippetAttributes(
      description = description
    )
  end attrs

  val snippet = db.add(attrs)

  val template = cli.template match
    case None =>
      promptTemplate(prompts, config.templates)
    case Some(value) =>
      Some(
        config.templates
          .find(_.name == value)
          .getOrElse(sys.error(s"Template '$value' not found"))
      )

  val snippetBase = files.prepare(snippet.id)

  template.foreach: tpl =>
    val snippetFiles = tpl.files.map(f => f.name -> f.content).toMap
    files.render(tpl, snippetBase, config.globalFiles)
    db.addFilesToSnippet(
      snippet.id,
      snippetFiles
    )

  import trigs.Progress
  given Progress = Progress.Quiet

  codesearch.withUpdater(empty = false): updater =>
    template.foreach: tpl =>
      updater(
        UpdateAction.Reindex,
        tpl.files
          .map(_.name)
          .map(p => snippetBase / os.RelPath(p))
      )

  Result.Open(files.resolve(snippet.id))
end commandNew
