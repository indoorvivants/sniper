package sniper

import decline_derive.*
import cue4s.*

@Help("Sniper is a command-line tool for managing code snippets.")
@Name("sniper")
enum CLI derives CommandApplication:
  @Help("Create a new snippet") case New(
      @Short("d")
      @Help("Description of the snippet, between 10 and 240 characters long")
      description: Option[String],
      @Short("t")
      @Help(
        "Template to use when creating snippet folder (if not provided, a picker will be used)"
      )
      template: Option[String]
  )
  @Help("Open a picker for existing snippets") case Open

  @Help("Delete selected snippets") case Delete

  @Name("search-code") @Help(
    "search code "
  ) case SearchCode(
      @Short("q") query: Option[String],
      @Short("l") limit: Option[Int]
  )

  case Sync

  @Name("print-config") @Help(
    "(for debugging) pretty print the configuration"
  ) case PrintConfig

  @Name("test-template") @Help(
    "(for debugging) test templates specified in configuration"
  ) case TestTemplate(
      @Short("n") name: Option[String],
      @Short("a") all: Boolean
  )
end CLI

@main def snippets(args: String*) =
  setupScribe()
  val defaultLocations = DefaultLocations(os.home)
  val config = Config.load(defaultLocations)
  val files = Files(config)
  val dbConfig = SnippetsDB.Config(config.db)
  val codesearchConfig = CodeSearch.Config(defaultLocations.codesearchIndex, config.data)
  val codesearch = CodeSearch(codesearchConfig)
  SnippetsDB.use(dbConfig): db =>
    Prompts.sync
      .withOutput(StderrOutput)
      .use: prompts =>
        val context = Context(config, files, db, prompts, codesearch)
        CommandApplication.parseOrExit[CLI](args) match
          case cli: CLI.New             => commandNew(context, cli)
          case CLI.Open                 => commandOpen(context)
          case CLI.Delete               => commandDelete(context)
          case CLI.Sync               => commandSync(context)
          case cli: CLI.SearchCode      => commandSearchCode(context, cli)
          case CLI.PrintConfig          =>
            scribe.info(s"Config from [${defaultLocations.configFile}]")
            pprint.pprintln(config)
          case cli: CLI.TestTemplate => commandTestTemplate(context, cli)
        end match

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
    prompts: SyncPrompts,
    codesearch: CodeSearch
)
