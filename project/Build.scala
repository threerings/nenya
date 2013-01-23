import sbt._
import Keys._
import samskivert.ProjectBuilder

object NenyaBuild extends Build {
  val builder = new ProjectBuilder("pom.xml") {
    override val globalSettings = Seq(
      crossPaths   := false,
      scalaVersion := "2.9.2",
      javacOptions ++= Seq("-Xlint", "-Xlint:-serial", "-source", "1.6", "-target", "1.6"),
      javaOptions ++= Seq("-ea"),
      fork in Compile := true,
      autoScalaLibrary := false, // no scala-library dependency
      publishArtifact in (Compile, packageDoc) := false, // no scaladocs; it fails
      // no parallel test execution to avoid confusions
      parallelExecution in Test := false,
      // TODO: nix this when Narya 1.13 is up on central
      resolvers += ".m2" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      // everybody needs JUnit wiring
      libraryDependencies ++= Seq(
        "com.novocode" % "junit-interface" % "0.7" % "test->default"
      )
    )
    override def projectSettings (name :String) = name match {
      case _ => Nil
    }
  }

  lazy val core = builder("core")
  lazy val tools = builder("tools")
  // one giant fruit roll-up to bring them all together
  lazy val nenya = Project("nenya", file(".")) aggregate(core, tools)
}
