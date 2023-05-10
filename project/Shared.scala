import java.nio.file.{Files, Path, Paths}

object Shared {

  val AnonCredsTag = "v0.1.0-15"
  val AnonCredsLibArchiveName = "library-linux-x86_64.tar.gz"
  val AnonCredsLibName = "libanoncreds.so"
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
  val AnonCredsLibDownloadUrl = s"${AnonCredsRepoBaseUrl}/releases/download/${AnonCredsTag}/${AnonCredsLibArchiveName}"
  val AnonCredsHeaderDownloadUrl =
    s"https://raw.githubusercontent.com/hyperledger/anoncreds-rs/${AnonCredsTag}/include/${AnonCredsLibHeaderName}"

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

  def downloadAndExtractAnonCredsSharedObject: Unit = {
    if (targetPathForAnoncredsSharedObjectDownload.resolve(AnonCredsLibName).toFile.exists()) {
      println(s"$AnonCredsLibName exists in $targetPathForAnoncredsSharedObjectDownload, no need to download again.")
    } else {
      println(s"Downloading $AnonCredsLibDownloadUrl to $tempPathForSharedObject.")
      Download.get(AnonCredsLibDownloadUrl, tempPathForSharedObject) match {
        case Left(httpErrorCode) =>
          println(s"Error code from download: $httpErrorCode")

        case Right(pathToZip) =>
          println(s"Unzip $pathToZip to $targetPathForAnoncredsSharedObjectDownload.")
          GnuUnZip.unzip(pathToZip, targetPathForAnoncredsSharedObjectDownload)
      }
    }
  }

  private def toStandardString(s: String): String = s.toLowerCase.replace("\\s+", "_")

  def pathToNativeObjectsInJar: Path =
    Path.of("NATIVE", toStandardString(sys.props("os.arch")), toStandardString(sys.props("os.name")))

  val NameOfShimSharedObject = "libanoncreds-shim.so"

}
