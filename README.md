# Sniper

<!--toc:start-->
- [Sniper](#sniper)
  - [Getting started](#getting-started)
    - [Installation](#installation)
    - [Usage](#usage)
    - [Configuration](#configuration)
    - [Templates](#templates)
  - [Integrations](#integrations)
    - [Shell completions](#shell-completions)
    - [Alfred workflow](#alfred-workflow)
    - [Model Context Protocol](#model-context-protocol)
<!--toc:end-->

Snippet management tool for the terminal.

**This software is classified as _blabware_ – designed primarily for a talk or a blog post. Even though I daily
drive it, it's not advertised to wider public and it follows my personal workflow. That said, if you find it useful and want to fix some bugs**

## Getting started

### Installation

- Via Homebrew: `brew install indoorvivants/tap/sniper`

- Via Coursier: `cs install sniper --channel https://cs.indoorvivants.com/i.json`

- Via GitHub releases: download the latest binary for your platform
  
- From source: clone the repository and run `make install`. 
  
  Requires [Scala CLI](https://scala-cli.virtuslab.org/) and [Clang](https://clang.llvm.org/) to be installed

### Usage

- Create a new snippet with `sniper new` – this will output the path to the snippet folder to STDOUT. 
  
  Recommended usage: `cd $(sniper new) && vim .`, where you can replace `vim` with your favorite editor

- Open existing snippet with `sniper open`. Same deal as with `new`, it outputs the path to the snippet folder. 
  
  Recommended usage: `cd $(sniper open) && vim .`

- Code search: `sniper search-code`. Note: currently the search results aren't great

- Synchronise code search index with snippets database: `sniper sync`.
  
  It's best to run this as a cron job.

- Delete a snippet with `sniper delete`. It will ask for confirmation before deleting the snippet folder

- Test your templates with `sniper test-template`

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

### Alfred workflow

https://www.alfredapp.com/

`sniper alfred workflow > hello.alfredworkflow`

Then open that `hello.alfredworkflow` file to add it to your installation

### Model Context Protocol

Sniper comes with a built-in [MCP](https://www.alfredapp.com/) server, using [STDIO transport](https://modelcontextprotocol.io/docs/concepts/transports#standard-input%2Foutput-stdio).

Launch it with `sniper mcp`, and use this command to add configuration to your favourite MCP-enabled tool.
