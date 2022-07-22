package io.isomarcte.sbt.aws.code.artifact.plugin

import _root_.io.isomarcte.sbt.aws.code.artifact.core._
import _root_.io.isomarcte.sbt.aws.code.artifact.io._
import cats.effect._
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import sbt.Keys._
import sbt.internal.util.ManagedLogger
import sbt.{IO => _, _}

object CodeArtifactPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  implicit private[this] lazy val ioRuntime: IORuntime = cats.effect.unsafe.implicits.global

  object autoImport extends Keys
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] =
    List(
      awsCodeArtifactAuthTokenForRepoFunction := { repo =>
        CodeArtifactCredentialsResolver.awsCli[IO](repo).attempt.map(_.leftMap(_.getLocalizedMessage)).unsafeRunSync
      }
    )

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      credentials ++= {
        val s: ManagedLogger                                             = streams.value.log
        val f: CodeArtifactRepo => Either[String, CodeArtifactAuthToken] = awsCodeArtifactAuthTokenForRepoFunction.value
        val g: CodeArtifactRepo => IO[CodeArtifactAuthToken] = { repo =>
          IO(f(repo)).flatMap(_.fold(e => IO.raiseError(new RuntimeException(e)), value => IO.pure(value)))
        }

        val allResolvers: Vector[Resolver] = resolvers.value.toVector ++ publishTo.value.toVector
        val repos: Set[CodeArtifactRepo]   = CodeArtifact.resolversToCodeArtifactRepos(allResolvers)

        val credentials: Set[Credentials] =
          CodeArtifact
            .resolveCredentials(g)(repos.toVector)
            .flatMap(
              _.fold(
                errors =>
                  IO(s.err("Unable to fetch credentials for any configured CodeArtifact repos.")) *>
                    errors.traverse_ { case (error, repo) =>
                      IO(s.err(s"Repo $repo, failed with: $error"))
                    } *> IO.pure(Set.empty[Credentials]),
                IO.pure,
                (errors, creds) =>
                  IO(s.err("Unable to fetch some credentials for CodeArtifact repos.")) *>
                    errors.traverse_ { case (error, repo) =>
                      IO(s.err(s"Repo $repo, failed with: $error"))
                    } *> IO.pure(creds)
              )
            )
            .attempt
            .unsafeRunSync match {
            case Left(e) =>
              s.err(s"Error while fetching CodeArtifact repo credentials: ${e}")
              Set.empty[Credentials]
            case Right(creds) =>
              creds
          }

        credentials.toList
      }
      // publishTo := {
      //   val original: Option[Resolver] = publishTo.?.value.getOrElse(None)
      //   val publish: Option[(String, CodeArtifactRepo)] = awsCodeArtifactPublishRepo.?.value
      //   val snapshot: Option[(String, CodeArtifactRepo)] = awsCodeArtifactSnapshotPublishRepo.?.value

      //   val target: Option[(String, CodeArtifactRepo)] =
      //     if (isSnapshot.value) {
      //       snapshot
      //     } else {
      //       publish
      //     }

      //   target.fold(original){
      //     case (name, repo) => Some(name at repo.url)
      //   }
      // },
      // resolvers ++= {
      //   val base: Vector[Resolver] = awsCodeArtifactResolverRepos.value.toVector.foldMap{
      //     case (name, repo) => Vector(name at repo.url)
      //   }
      //   val publishBase: Vector[Resolver] = awsCodeArtifactPublishRepo.?.value.toVector.map{
      //     case (name, repo) => name at repo.url
      //   }
      //   val snapshotBase: Vector[Resolver] = awsCodeArtifactSnapshotPublishRepo.?.value.toVector.map{
      //     case (name, repo) => name at repo.url
      //   }

      //   val publish: Vector[Resolver] =
      //     if (awsCodeArtifactUsePublishInResolvers.value) {
      //       publishBase
      //     } else {
      //       Vector.empty
      //     }

      //   val snapshot: Vector[Resolver] =
      //     if (awsCodeArtifactUseSnapshotInResolvers.value) {
      //       snapshotBase
      //     } else {
      //       Vector.empty
      //     }

      //   base ++ publish ++ snapshot
      // }
    )
}
