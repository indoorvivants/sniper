package sniper

import com.augustnagro.magnum.*

case class SnippetAttributes(
    description: String
)

@Table(SqliteDbType, SqlNameMapper.CamelToSnakeCase)
case class Snippet(
    @Id id: Long,
    description: String
) derives DbCodec

case class SnippetFileAttributes(
    snippetId: Long,
    filename: String,
    code: String
)

@Table(SqliteDbType, SqlNameMapper.CamelToSnakeCase)
case class SnippetFile(
    @Id id: Long,
    snippetId: Long,
    filename: String,
    code: String
) derives DbCodec

object SnippetFile:
  val Table = TableInfo[SnippetFileAttributes, SnippetFile, Long]

class SnippetsDB private (using DbCon):
  private val snippetsRepo = Repo[SnippetAttributes, Snippet, Long]
  private val snippetFilesRepo = Repo[SnippetFileAttributes, SnippetFile, Long]
  def getAll(): Vector[Snippet] =
    snippetsRepo.findAll

  def get(id: Long): Option[Snippet] =
    snippetsRepo.findById(id)

  private def randomString() =
    sql"select random()".query[Int].run().head.toString

  def add(attrs: SnippetAttributes): Snippet =
    val rand = randomString()
    snippetsRepo.insertReturning(attrs)
  end add

  def delete(id: Long) =
    val files = getFiles(id)
    files.foreach(f => snippetFilesRepo.deleteById(f.id))
    snippetsRepo.deleteById(id)

  def getFiles(snippetId: Long): Vector[SnippetFile] =
    sql"select ${SnippetFile.Table.all} from ${SnippetFile.Table} where snippet_id = $snippetId"
      .query[SnippetFile]
      .run()

  def addFilesToSnippet(
      snippetId: Long,
      files: Map[String, String]
  ): Vector[SnippetFile] =
    val sorted = files.toVector.sortBy(_._1)
    sorted.map { case (filename, code) =>
      val attrs = SnippetFileAttributes(snippetId, filename, code)
      snippetFilesRepo.insertReturning(attrs)
    }
  end addFilesToSnippet
end SnippetsDB

object SnippetsDB:
  case class Config(path: os.Path)
  def use[A](config: Config)(f: SnippetsDB => A) =
    val dataSource = new org.sqlite.SQLiteDataSource()
    dataSource.setUrl(s"jdbc:sqlite:${config.path}")

    // Scala Native doesn't support `java.lang.System.Logger` yet so we use a custom `SqlLogger`
    val xa = Transactor(dataSource, sqlLogger)

    connect(xa):
      sql"""
      CREATE TABLE IF NOT EXISTS snippet (id INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT, uid TEXT)
      """.update
        .run()
      sql"""
      CREATE TABLE IF NOT EXISTS snippet_file (id INTEGER PRIMARY KEY AUTOINCREMENT, snippet_id NUMBER, filename TEXT, code TEXT, uid TEXT)
      """.update
        .run()
      f(new SnippetsDB)
  end use
end SnippetsDB

private val sqlLogger = new SqlLogger:
  def exceptionMsg(exceptionEvent: SqlExceptionEvent): String =
    s"""Error executing query:
     |${exceptionEvent.sql}
     |With message:
     |${exceptionEvent.cause}
     |""".stripMargin

  def log(successEvent: SqlSuccessEvent): Unit =
    scribe.debug(
      s"Executed Query in ${successEvent.execTime}\n${successEvent.sql}".stripMargin
    )
