package io.isomarcte.sbt.aws.code.artifact.plugin

import io.isomarcte.sbt.aws.code.artifact.core._
import sbt._

trait Keys {

  final val awsCodeArtifactAuthTokenForRepoFunction
    : SettingKey[CodeArtifactRepo => Either[String, CodeArtifactAuthToken]] =
    settingKey[CodeArtifactRepo => Either[String, CodeArtifactAuthToken]](
      "A function which attempts to get an auth token for a given code artifact repo. By default, this will attempt to dynamically resolve the credentials for the repo from AWS. If you want to provide an alternative, e.g. an environment variable, a constant, etc, you must override this."
    )
}
