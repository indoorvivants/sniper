import com.indoorvivants.detective.Platform
import scala.scalanative.build.*

scalaVersion := "3.8.3"

enablePlugins(ScalaNativePlugin)
enablePlugins(ForgeNativeBinaryPlugin)

// The following two settings are to enable as flat a layout as possible (Scala files directly in src/ not in src/main/scala/)
Compile / unmanagedSourceDirectories := Seq(
  (ThisBuild / baseDirectory).value / "src"
)
Compile / unmanagedResourceDirectories := Seq(
  (ThisBuild / baseDirectory).value / "resources"
)

libraryDependencies ++= Seq(
  "com.github.lolgab" %% "magnum" % "1.3.1",
  "com.github.lolgab" %% "scala-native-jdbc-sqlite" % "0.0.5",
  "com.indoorvivants" %% "decline-completion" % "0.1.0",
  "com.indoorvivants" %% "decline-derive" % "0.3.6",
  "com.indoorvivants" %% "mcp-quick" % "0.2.0",
  "com.indoorvivants" %% "toml" % "0.3.0",
  "com.indoorvivants" %% "trigs" % "0.0.2",
  "com.lihaoyi" %% "os-lib" % "0.11.8",
  "com.lihaoyi" %% "pprint" % "0.9.6",
  "com.lihaoyi" %% "upickle" % "4.4.3",
  "com.outr" %% "scribe" % "3.19.0",
  "tech.neander" %% "cue4s" % "0.0.11"
)

buildBinaryConfig ~= { _.withName("sniper") }

nativeLink / nativeConfig ~= {
  import scala.scalanative.build.*

  _.withEmbedResources(true)
    .withResourceIncludePatterns(
      Seq("/Sniper.alfredworkflow")
    )
    .withIncrementalCompilation(true)
    .withSourceLevelDebuggingConfig(SourceLevelDebuggingConfig.enabled)
}

// this doesn't actually work but it makes me feel good about thinking
// that this could ever possibly work
nativeLinkReleaseFast / nativeConfig ~= {
  _.withLTO(if Platform.os == Platform.OS.MacOS then LTO.full else LTO.thin)
}

scalacOptions ++= Seq("-language:strictEquality", "-language:experimental.strictEqualityPatternMatching")

inThisBuild(
  List(
    homepage := Some(url("https://github.com/indoorvivants/sniper")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "indoorvivants",
        "Anton Sviridov",
        "contact@indoorvivants.com",
        url("https://blog.indoorvivants.com")
      )
    ),
    version := (if (!sys.env.contains("CI")) "dev" else version.value),
    crossScalaVersions := Nil
  )
)
