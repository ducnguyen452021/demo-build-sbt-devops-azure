import sbt.Keys.{isSnapshot, publishTo}
import sbt.addCompilerPlugin

import ReleaseTransformations._

ThisBuild / organization := "com.bizone"
//ThisBuild / version := "0.0.4"
ThisBuild / scalaVersion := "2.12.10"

crossScalaVersions := Seq("2.11.11", "2.12.3")
autoCompilerPlugins := true
lazy val commonSettings = Seq(
  name := "velocity",
  crossPaths := false,
  autoScalaLibrary := false,
  packageBin in Compile := baseDirectory.value / "target" / s"${name.value}-${version.value}.jar",
  //  packageDoc in Compile     := baseDirectory.value / s"${name.value}-javadoc.jar",
  //   disable publishing the main API jar
  Compile / packageDoc / publishArtifact := false,

  //   disable publishing the main sources jar
  Compile / packageSrc / publishArtifact := false,
  scalacOptions ++= Seq(
    "-encoding",
    "utf8",
    "-deprecation",
    "-feature",
    "-language:dynamics",
    "-language:reflectiveCalls",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-unchecked",
    "-target:jvm-1.8",
    s"-Xplugin:${baseDirectory.value} /target/${name.value}-${version.value}.jar",
    "-P:linter:printWarningNames:false",
    "-P:linter:enable-only:UseHypot+CloseSourceFile+OptionOfOption"
  ),
  fork := true,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
)
// common dependencies
ThisBuild / libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.1",
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "junit" % "junit" % "4.11" % Test,
  "org.mockito" %% "mockito-scala" % "1.16.37" % "test"
)

lazy val Prod = config("prod").extend(Compile).describedAs("scope to build production packages")
lazy val Dev = config("dev").extend(Compile).describedAs("scope to build dev packages")
// the application
lazy val app = project
  .in(file("."))
  .configs(Prod, Dev)
  .settings(commonSettings: _*)
  .settings(addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17"): _*)
  .settings(
    inConfig(Dev)(
      Classpaths.configSettings ++ Defaults.configTasks ++ baseAssemblySettings ++ Seq(
        assemblyJarName := s"${name.value}-${version.value}.jar",
        assemblyMergeStrategy in assembly := {
          case PathList("application.json") => MergeStrategy.discard
          case PathList("dev.json")         => new MyMergeStrategy()
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        }
      )
    )
  )
  .settings(
    inConfig(Prod)(
      Classpaths.configSettings ++ Defaults.configTasks ++ baseAssemblySettings ++ Seq(
        assemblyJarName := s"${name.value}-${version.value}.jar",
        assemblyMergeStrategy in assembly := {
          case PathList("application.json") => MergeStrategy.discard
          case PathList("prod.json")        => new MyMergeStrategy()
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        }
      )
    )
  )

coverageMinimum := 0

coverageFailOnMinimum := true

coverageHighlighting := true

publishMavenStyle := true

//publishTo := {
//  if (isSnapshot.value)
//    Some(MavenCache("Sonatype OSS Snapshots", file(Path.userHome.absolutePath + "/.m2/repository/snapshots")))
//  else
//    Some(MavenCache("local-maven", file(Path.userHome.absolutePath + "/.m2/repository")))
//}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
publishTo := {
  if (isSnapshot.value)
    Some(
      "snapshots".at(
        "https://bizonedev.pkgs.visualstudio.com/Demo/_packaging/maven_evaluation/maven/v1/snapshots"
      )
    )
  else
    Some(
      "release".at(
        "https://bizonedev.pkgs.visualstudio.com/Demo/_packaging/maven_sbt_demo/maven/v1/"
      )
    )
}

releaseIgnoreUntrackedFiles := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runClean, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion,
  commitReleaseVersion,
  pushChanges, //to make sure develop branch is pulled && will merge into master and push
  tagRelease,
  //  setNextVersion,
  //  commitNextVersion,
  pushChanges
)

releaseUseGlobalVersion := false
