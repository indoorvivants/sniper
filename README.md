# Sniper

Snippet management tool for the terminal.

**This software is classified as _blabware_ – designed primarily for a talk or a blog post. Even though I daily
drive it, it's not advertised to wider public and it follows my personal workflow. That said, if you find it useful and want to fix some bugs**

## Features

### Configuration

Sniper stores its config in a TOML file. Find its location 
by running `sniper print-config -l`.

### Templates
Define your own templates for snippets in the config file like so:

```toml
[[templates]]
name = 'scala3'
files = [{ name = "main.scala", content = """
//> using scala 3.7

@main def hello=
   println(42)
""" }]
references = ["scalafmt", "gitignore"]
test = ["scala-cli", "run", "."]
```

The `references` field allows you to define named files that you can share across templates:

```toml
[[globalFiles]]
label = "scalafmt"
filename = ".scalafmt.conf"
content = """
version = "3.9.6"
runner.dialect = scala3
rewrite.scala3.insertEndMarkerMinLines = 5
rewrite.scala3.removeOptionalBraces = true
rewrite.scala3.convertToNewSyntax = true"""

[[globalFiles]]
label = "gitignore"
filename = ".gitignore"
content = """
*.class
*.tasty
.metals
.bloop
.bsp
.scala-build
.sbt"""
```

## Integrations

### Shell completions
- **Bash**: `sniper completion bash > completions.bash && source completions.bash`
- **ZSH**: `sniper completion zsh > completions.zsh && source completions.zsh`

### [Alfred](https://www.alfredapp.com/) workflow

`sniper alfred workflow > hello.alfredworkflow`

Then open that `hello.alfredworkflow` file to add it to your installation

### Model Context Protocol

Sniper comes with a built-in [MCP](https://www.alfredapp.com/) server, using [STDIO transport](https://modelcontextprotocol.io/docs/concepts/transports#standard-input%2Foutput-stdio).

Launch it with `sniper mcp`, and use this command to add configuration to your favourite MCP-enabled tool.
