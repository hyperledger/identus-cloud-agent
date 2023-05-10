import java.nio.file.Path
import scala.sys.process.Process
import scala.language.postfixOps

object GnuUnZip {

  def unzip(zipPath: Path, outputPath: Path): Unit = {
    Process("tar" :: "xzf" :: zipPath.toString :: "-C" :: outputPath.toString :: Nil) !
  }
}
