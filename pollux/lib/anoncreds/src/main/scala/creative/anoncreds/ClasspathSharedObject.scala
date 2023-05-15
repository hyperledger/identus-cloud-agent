package creative.anoncreds

import buildinfo.BuildInfo

import java.nio.file.{Files, Path}

object ClasspathSharedObject {

  def removeLibPrefixAndSuffix(libFileName: String): String =
    libFileName.substring("lib".length, libFileName.length - ".so".length)

  def namesOfSharedObjectsToLoad: Seq[String] = Seq(
    removeLibPrefixAndSuffix(BuildInfo.NameOfAnonCredsSharedObject),
    removeLibPrefixAndSuffix(BuildInfo.NameOfShimSharedObject)
  )

  def createTempFolderWithExtractedLibs: Path = {
    val result = Files.createTempDirectory(".janoncreds")
    val pathToAnonCredsSO =
      Path.of("/", BuildInfo.pathToNativeObjectsInJar).resolve(BuildInfo.NameOfAnonCredsSharedObject)
    val pathToAnonCredsShimSO =
      Path.of("/", BuildInfo.pathToNativeObjectsInJar).resolve(BuildInfo.NameOfShimSharedObject)
    extractToTempFile(pathToAnonCredsSO, result)
    extractToTempFile(pathToAnonCredsShimSO, result)
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
