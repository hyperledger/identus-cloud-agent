package io.iohk.atala.pollux.anoncreds

import buildinfo.BuildInfo

import java.nio.file.{Files, Path}

object ClasspathUtils {
  enum OS {
    case WINDOWS extends OS
    case UNIX extends OS
    case MAC extends OS
    // case POSIX_UNIX extends OS
    case UnknownOS extends OS
    case MissingOS extends OS
  }

  def getOS: OS = Option(System.getProperty("os.name"))
    .map(_.toLowerCase match {
      case win if win.contains("windows") => OS.WINDOWS
      case linux
          if (linux.contains("linux") ||
            linux.contains("mpe/ix") ||
            linux.contains("freebsd") ||
            linux.contains("irix") ||
            linux.contains("digital unix") ||
            linux.contains("unix")) =>
        OS.UNIX
      case mac if mac.contains("mac os") => OS.MAC
      case posixUnix
          if (posixUnix.contains("sun os") ||
            posixUnix.contains("sunos") ||
            posixUnix.contains("solaris") ||
            posixUnix.contains("hp-ux") ||
            posixUnix.contains("aix")) =>
        OS.UNIX // TODO OS.POSIX_UNIX
      case any => OS.UnknownOS
    })
    .getOrElse(OS.MissingOS)
}

object ClasspathSharedObject {

  import ClasspathUtils._

  def namesOfSharedObjectsToLoad: Seq[String] = Seq("anoncreds", "anoncreds-shim")

  def createTempFolderWithExtractedLibs: Path = {
    val pathToNativeObjectsInJar: Path =
      getOS match {
        case OS.UNIX      => Path.of("/NATIVE/linux/amd64") // TODO Fix arch name for other similar arch
        case OS.MAC       => Path.of("/NATIVE/mac")
        case OS.WINDOWS   => ??? // TODO
        case OS.UnknownOS => ??? // TODO same as UNIX ???
        case OS.MissingOS => ???
      }

    val result = Files.createTempDirectory(".janoncreds")
    getOS match {
      case OS.UNIX =>
        extractToTempFile(pathToNativeObjectsInJar.resolve("libanoncreds.so"), result) // Side effect
        extractToTempFile(pathToNativeObjectsInJar.resolve("libanoncreds-shim.so"), result) // Side effect
      case OS.MAC =>
        extractToTempFile(pathToNativeObjectsInJar.resolve("libanoncreds.dylib"), result) // Side effect
        extractToTempFile(pathToNativeObjectsInJar.resolve("libanoncreds-shim.dylib"), result) // Side effect
      case OS.WINDOWS   => ??? // TODO
      case OS.UnknownOS => ??? // TODO same as UNIX ???
      case OS.MissingOS => ???
    }
    result
  }

  /** @param pathToResource
    *   full path to resource including file name
    * @return
    *   the path the file name is at (name included)
    */
  private def extractToTempFile(pathToResource: Path, tempPath: Path): Path = {
    val in = this.getClass.getResourceAsStream(pathToResource.toString)
    try {
      require(Option(in).isDefined, s"Cannot get resource $pathToResource as stream")
      val newLibFile = tempPath.resolve(pathToResource.getFileName)
      newLibFile.toFile.deleteOnExit()
      val byteCount = Files.copy(in, newLibFile)
      require(byteCount > 0, s"Copy of $pathToResource results in $byteCount bytes copied?")
      newLibFile
    } finally {
      Option(in) foreach (_.close())
    }

  }

}
