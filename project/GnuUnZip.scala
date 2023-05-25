import java.nio.file.{Files, Path}
import scala.language.postfixOps
import scala.sys.process.Process

object GnuUnZip {
  def unzip(zipPath: Path, outputPath: Path, newName: String): Unit = {
    val tmpOutputPath = Files.createTempDirectory("tmpUnzip")
    Process("tar" :: "xzf" :: zipPath.toString :: "-C" :: tmpOutputPath.toString :: Nil) !

    val dylibFile = tmpOutputPath.resolve("libanoncreds.dylib").toFile
    if (dylibFile.exists()) {
      dylibFile.renameTo(outputPath.resolve(newName).toFile)
    } else {
      println(s"File 'libanoncreds.dylib' not found in $tmpOutputPath")
    }
    tmpOutputPath.toFile.delete()
  }
}
