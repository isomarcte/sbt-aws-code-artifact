# SBT AWS Code Artifact #

**Note: This plugin is in active development and some things are rough. This includes docs, features, and tests :)**

This is a plugin for working with AWS [CodeArtifact][code-artifact]. CodeArtifact is a Maven compatible repository in AWS. It has some rough edges when interoping with sbt, due to sbt's use of ivy.

As of now, this plugin only does one thing. Automatically resolve your credentials.

# Credentials #

When working with AWS [CodeArtifact][code-artifact] it is common to use ephemerial credentials for both publishing and resolution, often timing out in less than 24 hours. This creates a terrible workflow for the developer.

To resolve this, this plugin scans all the configured resolvers. If any of them are [CodeArtifact][code-artifact] resolvers it will automatically use the [AWS CLI][aws-cli] to attempt to get new ephemerial credentials. This behavior can be overridden with the setting key `awsCodeArtifactAuthTokenForRepoFunction`.

# Known Issues #

* Publishing to [CodeArtifact][code-artifact] doesn't work out of the box. [CodeArtifact][code-artifact] only accepts maven style publishing and it is _very_ strict about the artifact naming. You can use [sbt-aether-deploy][sbt-aether-deploy] to deploy non-SNAPSHOT artifacts, but as of 2022-07-22, [sbt-aether-deploy][sbt-aether-deploy]'s syntax for SNAPSHOTs is rejected by [CodeArtifact][code-artifact] (I'm not sure who is at fault.).
* You won't get `maven-metadata.xml`.

[code-artifact]: https://aws.amazon.com/codeartifact/ "AWS CodeArtifact"

[aws-cli]: https://aws.amazon.com/cli/ "AWS CLI"

[sbt-aether-deploy]: https://github.com/arktekk/sbt-aether-deploy "SBT Aether Deploy"
