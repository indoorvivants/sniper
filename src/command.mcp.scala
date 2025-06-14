package sniper

import mcp.*

def commandMCP(context: Context): Result =
  val mcp = MCPBuilder
    .create()
    .handle(initialize): req =>
      InitializeResult(
        capabilities =
          ServerCapabilities(tools = Some(ServerCapabilities.Tools())),
        protocolVersion = req.protocolVersion,
        serverInfo = Implementation("sniper-mcp", "0.0.1")
      )
    .handle(tools.list): req =>
      ListToolsResult(
        Seq(
          Tool(
            name = "sniper_list",
            description =
              Some("Produce a list of snippets in the database in JSON format"),
            inputSchema = Tool.InputSchema()
          ),
          Tool(
            name = "sniper_code_search",
            description = Some(
              "Search snippet files for a given query, response is a list of json objects, containing line number, line contents, snippet ID, and file path"
            ),
            inputSchema = Tool.InputSchema(
              properties = Some(
                ujson.Obj(
                  "query" -> ujson.Obj("type" -> ujson.Str("string"))
                )
              ),
              required = Some(Seq("query"))
            )
          ),
          Tool(
            name = "sniper_read_file",
            description = Some(
              "Reads the contents of a file in a particular snippet. Contents are returned as raw text"
            ),
            inputSchema = Tool.InputSchema(
              properties = Some(
                ujson.Obj(
                  "snippetId" -> ujson.Obj("type" -> ujson.Str("integer")),
                  "filePath" -> ujson.Obj("type" -> ujson.Str("string"))
                )
              ),
              required = Some(Seq("snippetId", "filePath"))
            )
          )
        )
      )
    .handle(tools.call): req =>
      req.name match
        case "sniper_list" =>
          CallToolResult(
            Seq(TextContent(listSnippets(context)))
          )
        case "sniper_code_search" =>
          val query = req.arguments.get.obj("query").str
          val results = searchResults(context, query)
          CallToolResult(
            Seq(TextContent(upickle.default.write(results)))
          )

        case "sniper_read_file" =>
          val snippetId = req.arguments.get.obj("snippetId").num.toLong
          val filePath = req.arguments.get.obj("filePath").str
          if !context.db.get(snippetId).nonEmpty then
            Error(ErrorCode.InvalidRequest, s"Snippet $snippetId not found")
          else if !context.db.getFiles(snippetId).exists(_.filename == filePath)
          then
            Error(
              ErrorCode.InvalidRequest,
              s"File $filePath not found in snippet $snippetId"
            )
          else
            CallToolResult(
              Seq(
                TextContent(
                  os.read(
                    context.files.resolve(snippetId) / os.RelPath(filePath)
                  )
                )
              )
            )
          end if
    .run(SyncTransport.default)
  Result.None
end commandMCP

def listSnippets(context: Context) =
  upickle.default.write(
    ujson.Arr(
      context.db
        .getAll()
        .map(snip =>
          ujson.Obj(
            "snippetId" -> snip.id,
            "title" -> snip.description,
            "files" -> context.db.getFiles(snip.id).map(_.filename)
          )
        )
    )
  )
