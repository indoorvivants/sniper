package sniper

import decline_derive.*
import com.monovore.decline.Command

enum AlfredCommand derives CommandApplication:
  case Prepare(@Positional("query") query: String)
  case Create(@Positional("query") query: String)

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
  @Help("Open a picker for existing snippets") case Open(id: Option[Int])

  @Help("Delete selected snippets") case Delete

  @Help("Alfred workflow picker") case Alfred(
      command: AlfredCommand
  )

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

import com.monovore.decline.*
import cats.syntax.all.*
import net.andimiller.decline.completion.*

case class PrintCompletions(value: String)

given withCompletions[T](using
    t: CommandApplication[T]
): CommandApplication[T | PrintCompletions] =
  new:
    val newSubcommands =
      val bash =
        Command("bash", "output autocompletion script for bash")(
          Opts(
            PrintCompletions(Completion.bashCompletion(command))
          )
        )

      val zsh = Command("zsh", "output autocompletion script for zsh")(
        Opts(
          PrintCompletions(Completion.zshBashcompatCompletion(command))
        )
      )

      val completion = Command(
        "completion",
        "output autocompletion scripts for common shells"
      ) {
        Opts.subcommands(bash, zsh)
      }

      t.subcommands :+ completion
    end newSubcommands

    override def command: Command[T | PrintCompletions] =
      Command[T | PrintCompletions](t.command.name, t.command.header)(
        Opts.subcommands(newSubcommands.head, newSubcommands.tail*)
      )
    override def subcommands: List[Command[T | PrintCompletions]] =
      newSubcommands
