package snippets

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
      Option.when(s.length > 240)(
        PromptError(
          s"Description is ${s.length} characters long (max: 240)"
        )
      )
    )

  prompts.run(prompt).getOrThrow
end promptDescription
