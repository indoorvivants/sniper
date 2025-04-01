//> using dep com.indoorvivants.detective::platform::0.1.0

import com.indoorvivants.detective.Platform
import Platform.*

@main def coursierName = print(ArtifactNames.coursierString)
@main def ltoFlag =
  if os == Platform.OS.MacOS then "--native-lto full" else "--native-lto thin"

object ArtifactNames:
  lazy val coursierString = makeCoursierString(target)

  private def archString(bits: Platform.Bits, arch: Platform.Arch): String =
    (bits, arch) match
      case (Bits.x64, Arch.Intel) => "x86_64"
      case (Bits.x64, Arch.Arm)   => "aarch64"
      case (Bits.x32, Arch.Intel) => "x86_32"
      case (Bits.x32, Arch.Arm)   => "aarch32"

  private def osString(os: Platform.OS): String =
    import Platform.OS.*
    os match
      case Windows => "pc-win32"
      case MacOS   => "apple-darwin"
      case Linux   => "pc-linux"
      case Unknown => "unknown"
    end match
  end osString

  private def extString(os: Platform.OS): String =
    import Platform.OS.*
    os match
      case Windows => ".exe"
      case _       => ""
    end match
  end extString

  def makeCoursierString(target: Platform.Target): String =
    archString(target.bits, target.arch) +
      "-" +
      osString(target.os) +
      extString(target.os)
end ArtifactNames
