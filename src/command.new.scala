package sniper

def commandNew(context: Context, cli: CLI.New) =
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

  template.foreach: tpl =>
    val snippetFiles = tpl.files.map(f => f.name -> f.content).toMap
    files.render(tpl, files.prepare(snippet.id), config.globalFiles)
    db.addFilesToSnippet(
      snippet.id,
      snippetFiles
    )

  println(files.resolve(snippet.id))
end commandNew
