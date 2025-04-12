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

  cli match
    case AlfredCommand.Prepare(query) =>
      val templateQuery =
        query.split(" ", 2).toList.map(_.trim) match
          case templateName :: Nil =>
            response(
              ctx.config.templates
                .map(_.name)
                .filter(_.toLowerCase().contains(templateName))
                .map(name =>
                  AlfredItem(title = name, arg = name, valid = true)
                )*
            )
          case templateName :: snippetName :: Nil =>
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
                    arg = s"${value.name}:$normalisedSnippetName",
                    valid = true
                  )
                )
            end match

          case other => error(s"Unhandled case: $other")

    case AlfredCommand.Create(query) =>
      query.split(":", 2).toList.map(_.trim) match
        case templateName :: snippetName :: Nil =>
          commandNew(
            ctx,
            CLI.New(
              description = Some(snippetName),
              template = Some(templateName)
            )
          )
        case other => error(s"Invalid query argument $query")
  end match
end commandAlfred
