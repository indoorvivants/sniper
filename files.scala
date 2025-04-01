package sniper

class Files(config: Config):
  private val folder = config.data
  private inline def snippetFolder(id: Long) = folder / id.toString
  def delete(snippetId: Long) =
    if os.exists(snippetFolder(snippetId)) then
      os.remove.all(snippetFolder(snippetId))

  def create(snippetId: Long, files: Map[String, String]) =
    val base = snippetFolder(snippetId)
    if os.exists(base) then sys.error(s"$base already exists!")
    os.makeDir.all(base)
    files.foreach { case (name, content) =>
      os.write(base / name, content)
    }
  end create

  def render(
      template: Template,
      dir: os.Path,
      globalFiles: List[GlobalFile]
  ) =
    val files = globalFiles
      .filter(gf => template.references.contains(gf.label))
      .map(gf => (path = gf.filename, content = gf.content)) ++
      template.files.map(f => (path = f.name, content = f.content))

    files.foreach { case (path, content) =>
      scribe.info(s"Writing $path under $dir")
      os.write(dir / path, content)
    }
  end render

  def prepare(snippetId: Long): os.Path =
    val base = snippetFolder(snippetId)
    if os.exists(base) then sys.error(s"$base already exists!")
    os.makeDir.all(base)
    base
  end prepare

  def resolve(snippetId: Long) =
    if os.exists(snippetFolder(snippetId)) then snippetFolder(snippetId)
    else
      sys.error(
        s"Snippet folder ${{ snippetFolder(snippetId) }} does not exist!"
      )

end Files
