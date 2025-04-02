package sniper

import toml.*

case class TemplateFile(
    name: String,
    content: String
) derives Codec

case class GlobalFile(
    label: String,
    filename: String,
    content: String
) derives Codec

case class HiddenFile(
    name: String,
    content: String
) derives Codec

case class Template(
    name: String,
    files: List[TemplateFile] = Nil,
    hiddenFiles: List[HiddenFile] = Nil,
    references: List[String] = Nil,
    test: List[String] = Nil
) derives Codec

case class Config(
    db: os.Path,
    data: os.Path,
    globalFiles: List[GlobalFile] = Nil,
    templates: List[Template] = Nil
) derives Codec:
  def validate =
    val allRefs = globalFiles.map(_.label)
    templates.foreach: templ =>
      val refs = templ.references
      if !refs.forall(allRefs.contains) then
        scribe.error(
          s"Template ${templ.name} references unknown global file ${refs.find(!allRefs.contains(_)).get}. Available files: ${allRefs.mkString(", ")}"
        )
        sys.exit(-1)
      end if
    this
  end validate
end Config

object Config:
  def load(defaultDirs: DefaultLocations) =
    val configFile = defaultDirs.configFile
    if !os.exists(configFile) then
      scribe.warn(
        "Config file not found, creating one with defaults",
        configFile.toString
      )
      val dataDir = defaultDirs.share / "data"
      val dbFile = defaultDirs.state / "snippets.db"
      os.makeDir.all(dataDir)
      os.makeDir.all(configFile / os.up)
      os.makeDir.all(dbFile / os.up)

      val config = Config(db = dbFile, data = dataDir)
      os.write(configFile, s"db = '$dbFile'\ndata = '$dataDir'")
    end if

    Toml
      .parseAs[Config](os.read(configFile))
      .fold(
        (addr, msg) =>
          sys.error(
            s"Failed to read config file $configFile; at $addr: $msg"
          ),
        identity
      )
      .validate
  end load
end Config

private given Codec[os.Path] with
  def apply(
      value: Value,
      defaults: Codec.Defaults,
      index: Int
  ): Either[Parse.Error, os.Path] = value match
    case Value.Str(value) => Right(os.Path(value))
    case value =>
      Left((List.empty, s"String expected, $value provided"))
end given
