package sniper

import decline_derive.*, cue4s.*

enum Result:
  case Open(path: os.Path)
  case Out(contents: String)
  case None

  def print() =
    this match
      case Open(path)    => System.out.print(path)
      case Out(contents) => System.out.print(contents)
      case None          => ()
end Result

@main def snippets(args: String*) =
  setupScribe()
  val defaultLocations = DefaultLocations(os.home)
  val config = Config.load(defaultLocations)
  val files = Files(config)
  val dbConfig = SnippetsDB.Config(config.db)
  val codesearchConfig =
    CodeSearch.Config(defaultLocations.codesearchIndex, config.data)
  val codesearch = CodeSearch(codesearchConfig)
  val terminal = AnsiTerminal(StderrOutput)
  // scalafmt:off
  val result = SnippetsDB.use(dbConfig) { db =>
    Prompts.sync
      .withOutput(StderrOutput)
      .use[Result]: prompts =>
        val context = Context(
          config,
          files,
          db,
          prompts,
          codesearch,
          () => terminal.screenClear()
        )
        CommandApplication.parseOrExit[CLI | PrintCompletions](args) match
          case PrintCompletions(value) => Result.Out(value)
          case cli: CLI.New            => commandNew(context, cli)
          case cli: CLI.Open           => commandOpen(context, cli)
          case CLI.Delete              => commandDelete(context)
          case CLI.Sync                => commandSync(context)
          case cli: CLI.Alfred         => commandAlfred(context, cli.command)
          case cli: CLI.SearchCode     => commandSearchCode(context, cli)
          case CLI.PrintConfig =>
            scribe.info(s"Config from [${defaultLocations.configFile}]")
            Result.Out(pprint(config).toString)
          case cli: CLI.TestTemplate => commandTestTemplate(context, cli)
        end match
  }

  result.print()
  // scalafmt:on
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
    prompts: SyncPrompts,
    codesearch: CodeSearch,
    clearTerminal: () => Unit
)
