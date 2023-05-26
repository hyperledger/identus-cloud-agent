import java.nio.file.{Files, Path}
import scala.language.postfixOps
import scala.sys.process.Process

object GnuUnZip {
  def unzip(zipPath: Path, outputPath: Path, fileToExtract: String, newName: String): Unit = {
    // INFO just to show all the files
    val tmpInfo: Seq[String] = "tar" :: "-tvf" :: zipPath.toString :: Nil
    println(tmpInfo.mkString("RUN: \'", " ", "'"))
    Process(tmpInfo) !

    val tmpOutputPath = Files.createTempDirectory("tmpUnzip")

    // Extract all files
    val tmpExtract: Seq[String] = "tar" :: "xzf" :: zipPath.toString :: "-C" :: tmpOutputPath.toString :: Nil
    println(tmpExtract.mkString("RUN: \'", " ", "'"))
    Process(tmpExtract) !

    // val tmpExtract: Seq[String] =
    //   "tar" ::
    //     s"--transform='s/${fileToExtract.replace(".", "\\.")}/$newName/g'" ::
    //     "-zxvf" :: zipPath.toString :: "-C" :: (outputPath.toString) ::
    //     Nil
    // println(tmpExtract.mkString("RUN: \'", " ", "'"))
    // Process(tmpExtract) !
    // // RUN: 'tar --transform='s/libanoncreds\.so/libanoncreds-linux-x86_64.so/g' -zxvf /tmp/so_download1443181429681509984gzip -C pollux/lib/anoncreds/native-lib'
    // // tar: Invalid transform expression

    // Move the file
    val tmpMove: Seq[String] = "mv" ::
      tmpOutputPath.resolve(fileToExtract).toFile.toString ::
      outputPath.resolve(newName).toFile.toString ::
      Nil
    println(tmpMove.mkString("RUN: \'", " ", "'"))
    Process(tmpMove) !

    val libFile = tmpOutputPath.resolve(fileToExtract).toFile
    if (libFile.exists()) {
      libFile.renameTo(outputPath.resolve(newName).toFile) match {
        case true  => println(s"Rename file '${libFile}' to '${outputPath.resolve(newName).toFile}'")
        case false => println(s"[FAIL] to rename file '${libFile}' to '${outputPath.resolve(newName).toFile}'")
      }
    } else {
      println(s"File '$fileToExtract' not found in $tmpOutputPath")
    }

    tmpOutputPath.toFile.delete() match {
      case true  => println(s"Delete '${tmpOutputPath.toFile}'")
      case false => println(s"[FAIL] to delete '${tmpOutputPath.toFile}'")
    }

  }
}
