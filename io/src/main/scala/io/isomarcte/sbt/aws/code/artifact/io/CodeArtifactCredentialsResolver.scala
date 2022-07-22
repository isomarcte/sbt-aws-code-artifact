package io.isomarcte.sbt.aws.code.artifact.io

import cats.effect._
import cats.syntax.all._
import io.isomarcte.sbt.aws.code.artifact.core._
import scala.sys.process._

object CodeArtifactCredentialsResolver {

  def awsCli[F[_]](domain: CodeArtifactDomain, owner: CodeArtifactDomainOwner)(implicit
    F: Sync[F]
  ): F[CodeArtifactAuthToken] =
    F.blocking {
        val stdout: StringBuilder = new StringBuilder
        val stderr: StringBuilder = new StringBuilder
        val logger: ProcessLogger =
          new ProcessLogger {
            override def buffer[A](f: => A): A = f

            override def err(s: => String): Unit = {
              stderr.append(s);
              ()
            }

            override def out(s: => String): Unit = {
              stdout.append(s);
              ()
            }
          }

        (
          Process(
            "aws",
            List(
              "codeartifact",
              "get-authorization-token",
              "--domain",
              domain.value,
              "--domain-owner",
              owner.value,
              "--query",
              "authorizationToken",
              "--output",
              "text"
            )
          ) ! logger,
          stdout,
          stderr
        )
      }
      .flatMap {
        case (0, stdout, _) =>
          F.pure(CodeArtifactAuthToken(stdout.toString))
        case (ec, _, stderr) =>
          F.raiseError(
            new RuntimeException(
              s"Attempt to invoke aws cli to get code artifact auth code failed with exit code ($ec): ${stderr.toString}"
            )
          )
      }

  def awsCli[F[_]: Sync](codeArtifactRepo: CodeArtifactRepo): F[CodeArtifactAuthToken] =
    awsCli(codeArtifactRepo.domain, codeArtifactRepo.owner)
}
