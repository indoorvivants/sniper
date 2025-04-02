package sniper

import cue4s.*

object StderrOutput extends Output:
  override def logLn[A: AsString](a: A): Unit = ()
  override def out[A: AsString](a: A): Unit =
    System.err.print(a.render)
    System.err.flush()
end StderrOutput

def promptDescription(prompts: SyncPrompts): String =
  val prompt = Prompt
    .Input("Snippet description (max 240 characters)")
    .validate(s =>
      Option
        .when(s.length > 240)(
          PromptError(
            s"Description is too long (${s.length} characters, max: 240)"
          )
        )
        .orElse(
          Option.when(s.length < 10)(
            PromptError(
              s"Description is too short (${s.length} characters, min: 10)"
            )
          )
        )
    )

  prompts.run(prompt).getOrThrow
end promptDescription

def promptTemplate(
    prompts: SyncPrompts,
    templates: List[Template]
): Option[Template] =
  val prompt = prompts
    .singleChoice(
      "Select template to use",
      "No template" :: templates.map(_.name)
    )
    .getOrThrow

  templates.find(_.name == prompt)
end promptTemplate
