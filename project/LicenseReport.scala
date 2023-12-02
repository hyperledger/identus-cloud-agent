package io.iohk.atala.sbt

import sbt.Keys._
import sbt._
import sbt.librarymanagement
import sbt.librarymanagement._
import sbt.librarymanagement.ivy.IvyDependencyResolution
import sbtlicensereport.license

/** A plugin which enables reporting on licensing used within a project. */
object CustomSbtLicenseReport extends AutoPlugin {
  import sbtlicensereport.SbtLicenseReport

  override def requires: Plugins = SbtLicenseReport.requires
  override def trigger = SbtLicenseReport.trigger

  import SbtLicenseReport.autoImportImpl
  val autoImport = autoImportImpl
  import autoImport._

  private class CustomResolution(origResolution: DependencyResolution, logger: Logger)
      extends DependencyResolutionInterface {

    private val dummyFile = java.io.File.createTempFile("sbt-license-report", "")
    private val dummyStat = librarymanagement.UpdateStats(0, 0, 0, false)
    private val dummyReport = librarymanagement.UpdateReport(dummyFile, Vector.empty, dummyStat, Map.empty)

    override def moduleDescriptor(moduleSetting: ModuleDescriptorConfiguration): ModuleDescriptor =
      origResolution.moduleDescriptor(moduleSetting)

    override def update(
        module: ModuleDescriptor,
        configuration: UpdateConfiguration,
        uwconfig: UnresolvedWarningConfiguration,
        log: Logger
    ): Either[UnresolvedWarning, UpdateReport] = {
      log.error("using custom resolution update")
      origResolution
        .update(module, configuration, uwconfig, log)
        .left
        .flatMap(_ => Right(dummyReport))
    }
  }

  override lazy val projectSettings: Seq[Setting[_]] =
    SbtLicenseReport.projectSettings ++
      Seq(
        updateLicenses := {
          val ignore = update.value
          val overrides = licenseOverrides.value.lift
          val depExclusions = licenseDepExclusions.value.lift
          val originatingModule = DepModuleInfo(organization.value, name.value, version.value)
          val resolution = DependencyResolution(
            new CustomResolution(IvyDependencyResolution(ivyConfiguration.value), streams.value.log)
          )
          license.LicenseReport.makeReport(
            ivyModule.value,
            resolution,
            licenseConfigurations.value,
            licenseSelection.value,
            overrides,
            depExclusions,
            originatingModule,
            streams.value.log
          )
        },
      )

  override lazy val globalSettings = SbtLicenseReport.globalSettings
}
