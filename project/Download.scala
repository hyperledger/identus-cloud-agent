import java.net.{HttpURLConnection, URL}
import java.nio.file.Path
import scala.sys.process._

object Download {

  def get(urlOfFileToDownload: String, outputPath: Path): Either[Int, Path] = {
    val url = new URL(urlOfFileToDownload)

    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(50000)
    connection.connect()

    if (connection.getResponseCode >= 400)
      Left(connection.getResponseCode)
    else {

      val output = url #> outputPath.toFile !! ProcessLogger(println(_))
      println(output)
      Right(outputPath)
    }
  }
}
