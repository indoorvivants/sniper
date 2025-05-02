package sniper

import AlfredProtocol.*

def commandAlfred(ctx: Context, cli: AlfredCommand): Result =
  def handleIntro =
    response(
      Seq(
        AlfredItem(
          title = "new snippet (new or n)",
          arg = "",
          valid = false,
          autocomplete = "new "
        ),
        AlfredItem(
          title = "open snippet (open or o)",
          arg = "",
          valid = false,
          autocomplete = "open "
        ),
        AlfredItem(
          title = "code search (code or c)",
          arg = "",
          valid = false,
          autocomplete = "code "
        )
      )
    )

  def handleNew(args: Seq[String]) =
    args match
      case Nil =>
        response(
          ctx.config.templates
            .map(_.name)
            .map: msg =>
              AlfredItem(
                title = msg,
                valid = false,
                autocomplete = s"new $msg ",
                arg = ""
              )
        )
      case templateName :: Nil =>
        info(
          ctx.config.templates
            .map(_.name)
            .filter(_.toLowerCase().contains(templateName))
        )
      case templateName :: rest =>
        val snippetName = rest.mkString(" ")
        val normalisedTemplateName = templateName.trim.toLowerCase
        val normalisedSnippetName = snippetName.trim
        ctx.config.templates.find(_.name == normalisedTemplateName) match
          case None =>
            error(s"No template named `$normalisedTemplateName` found")
          case Some(value) =>
            response(
              AlfredItem(
                title =
                  s"Create snippet named `$normalisedSnippetName` using template `$normalisedTemplateName`",
                arg = s"new:${value.name}:$normalisedSnippetName",
                valid = true
              )
            )
        end match
    end match
  end handleNew

  def handleOpen(args: Seq[String]) =
    val all = ctx.db.getAll()
    val query = args.mkString(" ").toLowerCase.trim
    val filtered = all.filter(snip =>
      query.isEmpty() || snip.description.toLowerCase().contains(query)
    )

    response(
      filtered.map(snip =>
        AlfredItem(
          title = s"Open `${snip.description}`",
          arg = s"open:${snip.id}",
          valid = true
        )
      )
    )
  end handleOpen

  def handleSearch(args: Seq[String]) =
    val results = searchResults(ctx, args.mkString(" "))
    if results.isEmpty then error("No results :(")
    else

      val groupedResults =
        val all = Seq.newBuilder[AlfredItem]
        var curSnippetId: Long = -1
        val curGroupBuilder = List.newBuilder[SearchResults]

        def saveCurrent(): Unit =
          curGroupBuilder.result() match
            case results @ (h :: next) =>
              all += AlfredItem(
                title = h.snippetTitle,
                subtitle = results.take(5).map(_.line.trim).mkString("\n"),
                valid = true,
                arg = s"sc:${h.snippetId}:${h.path}"
              )
            case Nil =>

        results.foreach: sr =>
          if curSnippetId != sr.snippetId then
            saveCurrent()
            curGroupBuilder.clear()
            curSnippetId = sr.snippetId
          else curGroupBuilder += sr

        saveCurrent()

        all.result()
      end groupedResults

      response(groupedResults)
    end if
  end handleSearch

  cli match
    case AlfredCommand.Workflow =>
      val isTTY = scalanative.posix.unistd.isatty(1) == 1

      if isTTY then
        sys.error(
          "stdout is a TTY (a terminal) – please redirect the output of this command like this: " +
            "`sniper alfred workflow > Sniper.alfredworkflow && open Sniper.alfredworkflow`"
        )
      else
        val is =
          this.getClass().getResourceAsStream("/Sniper.alfredworkflow")
        val bytes = new Array[Byte](1024)
        var length = 0
        while { length = is.read(bytes); length } != -1 do
          System.out.write(bytes, 0, length)
        System.out.flush()
        is.close()
      end if
      Result.None

    case AlfredCommand.Prepare(query) =>
      val args = query.split(" ").toList.map(_.trim)

      args match
        case ("n" | "new") :: next =>
          handleNew(next)
        case ("o" | "open") :: next =>
          handleOpen(next)
        case ("c" | "code") :: next =>
          handleSearch(next)
        case _ =>
          handleIntro
      end match

    case AlfredCommand.Run(query) =>

      def openPath(path: os.Path) =
        val location = s"'${path.toString.replaceAll("'", raw"\'")}'"
        val command = ctx.config.alfred.opencommand
          .replace("{snippet_location}", location)

        Result.Out(command)
      end openPath

      query.split(":").toList.map(_.trim) match
        case "new" :: templateName :: rest =>
          openPath(
            commandNew(
              ctx,
              CLI.New(
                description = Some(rest.mkString(":")),
                template = Some(templateName)
              )
            ).path
          )
        case "open" :: id :: Nil =>
          openPath(
            commandOpen(
              ctx,
              CLI.Open(
                id = Some(id.toInt)
              )
            ).path
          )

        case "sc" :: id :: relPath :: Nil =>
          openPath(
            ctx.files.resolve(id.toLong)
          )

        case other => error(s"Invalid query argument $query")
      end match
  end match

end commandAlfred

object AlfredProtocol:
  case class AlfredItem(
      title: String,
      arg: String,
      valid: Boolean,
      subtitle: String = "",
      autocomplete: String = ""
  ) derives upickle.default.ReadWriter

  case class AlfredResponse(items: Seq[AlfredItem])
      derives upickle.default.ReadWriter

  import upickle.default.*

  def response(items: AlfredItem): Result = Result.Out(
    write(AlfredResponse(List(items)))
  )
  def response(items: Seq[AlfredItem]): Result = Result.Out(
    write(AlfredResponse(items))
  )

  def error(msg: String) = response(
    AlfredItem(s"ERROR: $msg", "", valid = false)
  )

  def info(msgs: Seq[String]) = response(
    msgs.map { msg =>
      AlfredItem(title = msg, arg = "", valid = false, autocomplete = msg)
    }
  )
end AlfredProtocol
