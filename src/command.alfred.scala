package sniper

case class AlfredItem(
    title: String,
    arg: String,
    valid: Boolean
) derives upickle.default.ReadWriter

case class AlfredResponse(items: List[AlfredItem])
    derives upickle.default.ReadWriter

import upickle.default.*
def commandAlfred(ctx: Context, cli: AlfredCommand) =
  def response(items: AlfredItem*) = print(
    write(AlfredResponse(items.toList))
  )

  def error(msg: String) = response(
    AlfredItem(s"ERROR: $msg", "", valid = false)
  )

  def info(msgs: Seq[String]) = response(
    msgs.map { msg =>
      AlfredItem(title = msg, arg = "", valid = false)
    }*
  )

  def handleIntro = info(Seq("Type new (or n), open (or o), code (or c)"))

  def handleNew(args: Seq[String]) =
    args match
      case Nil =>
        info(
          ctx.config.templates
            .map(_.name)
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
      )*
    )
  end handleOpen

  cli match
    case AlfredCommand.Prepare(query) =>
      val args = query.split(" ").toList.map(_.trim)

      args match
        case ("n" | "new") :: next =>
          handleNew(next)
        case ("o" | "open") :: next =>
          handleOpen(next)
        case ("c" | "code") :: next => ???
        case _ =>
          handleIntro
      end match

    case AlfredCommand.Create(query) =>
      query.split(":").toList.map(_.trim) match
        case "new" :: templateName :: rest =>
          commandNew(
            ctx,
            CLI.New(
              description = Some(rest.mkString(":")),
              template = Some(templateName)
            )
          )
        case "open" :: id :: Nil =>
          commandOpen(
            ctx,
            CLI.Open(
              id = Some(id.toInt)
            )
          )
        case other => error(s"Invalid query argument $query")
  end match

end commandAlfred
