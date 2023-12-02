import sbt.Logger
import sbt.librarymanagement._

class LicenseReportCustomDependencyResolution(origResolution: DependencyResolution)
    extends DependencyResolutionInterface {

  private val dummyFile = java.io.File.createTempFile("sbt-license-report", "")
  private val dummyStat = UpdateStats(0, 0, 0, false)
  private val dummyReport = UpdateReport(dummyFile, Vector.empty, dummyStat, Map.empty)

  override def moduleDescriptor(moduleSetting: ModuleDescriptorConfiguration): ModuleDescriptor =
    origResolution.moduleDescriptor(moduleSetting)

  override def update(
      module: ModuleDescriptor,
      configuration: UpdateConfiguration,
      uwconfig: UnresolvedWarningConfiguration,
      log: Logger
  ): Either[UnresolvedWarning, UpdateReport] = {
    origResolution
      .update(module, configuration, uwconfig, Logger.Null)
      .left
      .flatMap { unresolved =>
        log.warn(":::::::::::::::::::::::::::::::::::::::::::::::::::")
        log.warn("::     LicenseReport Unresolved Dependencies     ::")
        log.warn(":::::::::::::::::::::::::::::::::::::::::::::::::::")
        unresolved.failedPaths
          .flatMap(_.headOption)
          .map { case (module, _) => module.toString() }
          .sorted
          .distinct
          .foreach { module => log.warn(s":: $module") }
        log.warn(":::::::::::::::::::::::::::::::::::::::::::::::::::")
        Right(dummyReport)
      }
  }
}
