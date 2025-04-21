package sniper

def commandTestTemplate(context: Context, cli: CLI.TestTemplate) =
  import context.*
  if cli.name.isDefined && cli.all then
    sys.error("--all and --name are mutually exclusive")

  def test(template: Template) =
    val tempDir = os.temp.dir(prefix = "test-template")
    scribe.info(s"Testing template ${template.name} in ${tempDir}")
    files.render(template, tempDir, config.globalFiles)
    scribe.info(s"Running test command ${template.test.mkString(" ")}")
    os.call(
      template.test,
      cwd = tempDir,
      stdout = os.Inherit,
      stderr = os.Inherit
    )
    os.remove.all(tempDir)
    println(s"✅ Template ${template.name} tested")
  end test

  if cli.all then config.templates.foreach(test)
  else
    val template = cli.name match
      case None =>
        val name = prompts
          .singleChoice(
            "Select template to test",
            config.templates.map(_.name)
          )
          .getOrThrow

        config.templates.find(_.name == name).get
      case Some(value) =>
        config.templates
          .find(_.name == value)
          .getOrElse(sys.error("Template not found"))
    end template

    test(template)
  end if

  Result.None
end commandTestTemplate
