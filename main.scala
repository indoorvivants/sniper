package snippets

import decline_derive.*
import cue4s.*
import java.util.UUID
import os.temp
import os.group.set

enum CLI derives CommandApplication:
  case New(
      @Short("d")
      description: Option[String],
      @Short("t")
      template: Option[String]
  )
  case Open, Delete
  @Name("print-config") case PrintConfig()
  @Name("test-template") case TestTemplate(
      @Short("n") name: Option[String],
      @Short("a") all: Boolean
  )
end CLI

@main def snippets(args: String*) =
  setupScribe()
  val config = Config.load(os.home)
  val cli = CommandApplication.parseOrExit[CLI](args)
  val files = Files(config)
  SnippetsDB.use(SnippetsDB.Config(config.db)): db =>
    Prompts.sync
      .withOutput(StderrOutput)
      .use: prompts =>
        handle(Context(config, files, db, prompts), cli)

end snippets

def setupScribe() =
  scribe.Logger.root
    .clearHandlers()
    .withHandler(
      writer = scribe.writer.SystemErrWriter,
      outputFormat = scribe.output.format.ANSIOutputFormat
    )
    .replace()

case class Context(
    config: Config,
    files: Files,
    db: SnippetsDB,
    prompts: SyncPrompts
)

def handle(context: Context, cli: CLI) =
  import context.*
  cli match
    case CLI.New(desc, tpl) =>
      val attrs =
        val description = desc.getOrElse(promptDescription(prompts))

        SnippetAttributes(
          description = description
        )
      end attrs

      val snippet = db.add(attrs)

      val template = tpl match
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
    case CLI.Open =>
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

    case CLI.Delete =>
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
    case CLI.PrintConfig() =>
      pprint.pprintln(config)

    case CLI.TestTemplate(name, all) =>
      if name.isDefined && all then
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

      if all then config.templates.foreach(test)
      else
        val template = name match
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

  end match
end handle
