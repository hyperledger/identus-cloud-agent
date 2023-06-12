package creative.anoncreds

import scala.util.{Failure, Success, Try}
import io.iohk.atala.pollux.anoncreds.ClasspathSharedObject

object TestLoadFromJar {

  def main(args: Array[String]): Unit = {

    val api = Try(ClasspathSharedObject.createTempFolderWithExtractedLibs) match {
      case Failure(exception) =>
        val p = Seq("native-lib", ".")
        println(s"Failed to extract libs ($exception)")
        println(s"Using anoncreds path ... $p")
        AnonCreds(p)
      case Success(path) =>
        println(s"Using anoncreds path ... $path")
        AnonCreds(Seq(path.toString))
    }

    println(api.anoncreds_version())

  }
}
