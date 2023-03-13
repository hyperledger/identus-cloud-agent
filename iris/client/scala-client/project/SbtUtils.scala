object SbtUtils {

  import sbt._
  import sbt.internal.DslEntry.DslDisablePlugins

  def disablePlugins(f: Project => Project) = new DslDisablePlugins(Seq()) {
    override val toFunction: Project => Project = f
  }

}
