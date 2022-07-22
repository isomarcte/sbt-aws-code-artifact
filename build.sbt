import _root_.io.isomarcte.sbt.aws.code.artifact.build._
import _root_.io.isomarcte.sbt.aws.code.artifact.build.GAVs._

// Constants //

lazy val isomarcteOrg: String       = "io.isomarcte"
lazy val projectName: String        = "sbt-aws-code-artifact"
lazy val projectUrl: URL            = url(s"https://github.com/isomarcte/${projectName}")
lazy val scala212: String           = "2.12.16"
lazy val scalaVersions: Set[String] = Set(scala212)

// SBT Command Aliases //

// Usually run before making a PR
addCommandAlias(
  "full_build",
  ";+clean;githubWorkflowGenerate;+test;+test:doc;+unusedCompileDependenciesTest;+undeclaredCompileDependenciesTest;+Test/unusedCompileDependenciesTest;+Test/undeclaredCompileDependenciesTest;+versionSchemeEnforcerCheck;+scalafmtAll;+scalafmtSbt;+scalafixAll;+scripted;"
)

// ThisBuild //

// General

ThisBuild / versionScheme := Some("pvp")

ThisBuild / scalacOptions ++= List("-target:jvm-1.8")

ThisBuild / organization := isomarcteOrg
ThisBuild / scalafixDependencies ++= List(organizeImportsG %% organizeImportsA % organizeImportsV)
ThisBuild / scalafixScalaBinaryVersion := scalaBinaryVersion.value
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
// We only publish on 2.12.x to keep in line with SBT, but it is assumed that
// SBT will get to 2.13.x someday, so this ensures we stay up to date.
ThisBuild / crossScalaVersions := scalaVersions.toSeq
ThisBuild / versionSchemeEnforcerInitialVersion := Some("0.0.0.1")

// GithubWorkflow

ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowOSes := Set("macos-latest", "ubuntu-latest").toList
ThisBuild / githubWorkflowJavaVersions := List(8, 11, 17, 18).map(value => JavaSpec.temurin(value.toString))
ThisBuild / githubWorkflowBuildPreamble :=
  List(
    WorkflowStep.Sbt(List("scalafmtSbtCheck", "scalafmtCheckAll", "versionSchemeEnforcerCheck")),
    WorkflowStep.Run(List("sbt 'scalafixAll --check'")),
    WorkflowStep.Sbt(List("publishLocal")),
    WorkflowStep.Sbt(List("doc"))
  )
ThisBuild / githubWorkflowBuildPostamble := List(WorkflowStep.Sbt(List("test:doc")))

// Common Settings //

lazy val commonSettings: List[Def.Setting[_]] = List(
  scalaVersion := scala212,
  addCompilerPlugin(betterMonadicForG %% betterMonadicForA % betterMonadicForV),
  addCompilerPlugin(typelevelG         % kindProjectorA    % kindProjectorV cross CrossVersion.full)
)

// Publish Settings //

lazy val publishSettings = List(
  homepage := Some(projectUrl),
  licenses := Seq("BSD3" -> url("https://opensource.org/licenses/BSD-3-Clause")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus: String = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(ScmInfo(projectUrl, s"scm:git:git@github.com:isomarcte/${projectName}.git")),
  developers :=
    List(Developer("isomarcte", "David Strawn", "isomarcte@gmail.com", url("https://github.com/isomarcte"))),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)

// Root //

lazy val root: Project = (project in file("."))
  .settings(commonSettings, publishSettings)
  .settings(
    List(
      name := projectName,
      Compile / packageBin / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false
    )
  )
  .settings(inThisBuild(commonSettings))
  .aggregate(core, io, plugin)

// Core //

lazy val core: Project = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}-core",
    libraryDependencies ++=
      List(circeG %% circeCoreA % circeCoreV, typelevelG %% catsCoreA % catsV, typelevelG %% catsKernelA % catsV)
  )

// IO

lazy val io: Project = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}-io",
    libraryDependencies ++=
      List(
        typelevelG %% catsCoreA         % catsV,
        typelevelG %% catsEffectA       % catsEffectV,
        typelevelG %% catsEffectKernelA % catsEffectV
      )
  )
  .dependsOn(core)

// Plugin //

lazy val plugin: Project = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}-plugin",
    libraryDependencies ++=
      List(
        typelevelG %% catsCoreA                 % catsV,
        typelevelG %% catsEffectA               % catsEffectV,
        typelevelG %% catsEffectKernelA         % catsEffectV,
        typelevelG %% catsKernelA               % catsV,
        scalaSbtG   % sbtA                      % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtCollectionsA           % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtCoreMacrosA            % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtLibraryManagementCoreA % sbtLibraryManagementCoreV % Provided,
        scalaSbtG  %% sbtLibraryManagementIvyA  % sbtLibraryManagementCoreV % Provided,
        scalaSbtG  %% sbtMainA                  % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtMainSettingsA          % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtTaskSystemA            % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtUtilLoggingA           % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtUtilPositionA          % sbtVersion.value          % Provided
      ),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .enablePlugins(SbtPlugin)
  .disablePlugins(SbtVersionSchemeEnforcerPlugin)
  .dependsOn(core, io)
