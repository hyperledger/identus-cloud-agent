import java.nio.file.{Files, Path}

object Shared {

  val AnonCredsTag = "v0.1.0-dev.8"
  val AnonCredsLibName = "libanoncreds.so"
  def AnonCredsLibNameByOS = {
    System.getProperty("os.name").toLowerCase match {
      case "darwin"  => "libanoncreds.dylib"
      case "linux"   => "libanoncreds.so"
      case _: String => ??? // TODO
    }
  }
  val AnonCredsLibHeaderName = "libanoncreds.h"
  val TargetForAnoncredsSharedObjectDownloadFIXME = "native-lib"
  val TargetForAnoncredsSharedObjectDownload = "pollux/lib/anoncreds/native-lib" // "native-lib"
  val NativeCodeSourceFolder = "pollux/lib/anoncreds/src/main/c" // "src/main/c"

  def tempPathForSharedObject: Path = Files.createTempFile("so_download", "gzip")

  def targetPathForAnoncredsSharedObjectDownload: Path =
    Files.createDirectories(
      Path.of(TargetForAnoncredsSharedObjectDownload)
    )

  val AnonCredsRepoBaseUrl = "https://github.com/hyperledger/anoncreds-rs"
  val AnonCredsHeaderDownloadUrl =
    s"https://raw.githubusercontent.com/hyperledger/anoncreds-rs/${AnonCredsTag}/include/${AnonCredsLibHeaderName}"

  val MacArchs: Seq[String] = Seq("arm64", "x86_64")
  val MacArchsToDownloadName: Map[String, String] = Map("arm64" -> "aarch64")

  def newAnonCredsLibName(os: String, arch: String) = os match {
    case "darwin"  => s"libanoncreds-$os-$arch.dylib"
    case "linux"   => s"libanoncreds.so" // s"libanoncreds-$os-$arch.so"
    case _: String => ??? // TODO
  }

  def anonCredsLibFilePath(os: String, arch: String): String =
    s"$TargetForAnoncredsSharedObjectDownload/" + newAnonCredsLibName(os, arch)

  def anonCredsLibDownloadUrl(os: String, arch: String): String =
    s"$AnonCredsRepoBaseUrl/releases/download/$AnonCredsTag/library-$os-${MacArchsToDownloadName.getOrElse(arch, arch)}.tar.gz"

  def anonCredsLibLocation: String =
    targetPathForAnoncredsSharedObjectDownload.resolve(AnonCredsLibName).toString

  def anonCredsLibHeaderLocation: Path = Path.of(NativeCodeSourceFolder, AnonCredsLibHeaderName)

  def downloadSharedObjectHeaderFile: Unit = {
    // https://github.com/hyperledger/anoncreds-rs/blob/v0.1.0-dev.15/include/libanoncreds.h
    if (anonCredsLibHeaderLocation.toFile.exists()) {
      println(
        s"$anonCredsLibHeaderLocation exists, no download necessary. Delete this file to trigger download if you've changed the tag."
      )
    } else {
      println(s"Downloading $AnonCredsHeaderDownloadUrl to $anonCredsLibHeaderLocation.")
      Download.get(AnonCredsHeaderDownloadUrl, anonCredsLibHeaderLocation)
    }
  }

  def downloadAndExtractAnonCredsSharedObject(downloadUrl: String, os: String, arch: String): Unit = {
    if (targetPathForAnoncredsSharedObjectDownload.resolve(AnonCredsLibName).toFile.exists()) {
      println(s"$AnonCredsLibName exists in $targetPathForAnoncredsSharedObjectDownload, no need to download again.")
    } else {
      println(s"Downloading $downloadUrl to $tempPathForSharedObject.")
      def libDefualtFileName = os match {
        case "darwin"  => "libanoncreds.dylib"
        case "linux"   => "libanoncreds.so"
        case _: String => ??? // TODO
      }
      Download.get(downloadUrl, tempPathForSharedObject) match {
        case Left(httpErrorCode) =>
          println(s"Error code from download: $httpErrorCode")

        case Right(pathToZip) =>
          println(
            s"Unzip $pathToZip to '$targetPathForAnoncredsSharedObjectDownload'"
          )
          GnuUnZip.unzip(
            pathToZip,
            targetPathForAnoncredsSharedObjectDownload,
            libDefualtFileName,
            newAnonCredsLibName(os, arch)
          )
      }
    }
  }

  private def toStandardString(s: String): String = s.toLowerCase.replace("\\s+", "_")

  def pathToNativeObjectsInJar: Path =
    Path.of("NATIVE", toStandardString(sys.props("os.arch")), toStandardString(sys.props("os.name")))

  val NameOfShimSharedObject = "libanoncreds-shim.so"

}
