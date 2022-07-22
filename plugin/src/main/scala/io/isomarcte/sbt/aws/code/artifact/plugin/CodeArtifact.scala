package io.isomarcte.sbt.aws.code.artifact.plugin

import cats._
import cats.data._
import cats.syntax.all._
import io.isomarcte.sbt.aws.code.artifact.core._
import sbt._
import sbt.librarymanagement._
import scala.collection.immutable.SortedSet

private[plugin] object CodeArtifact {

  def nonEmptyResolveCredentials[F[_], G[_]](
    f: CodeArtifactRepo => F[CodeArtifactAuthToken]
  )(repos: G[CodeArtifactRepo])(implicit
    F: ApplicativeError[F, Throwable],
    G: NonEmptyTraverse[G],
    P: Parallel[F]
  ): F[Ior[NonEmptyList[(String, CodeArtifactRepo)], Set[Credentials]]] =
    Parallel.parReduceMapA(repos)(repo =>
      f(repo).redeem(
        e =>
          Ior.left[NonEmptyList[(String, CodeArtifactRepo)], Set[Credentials]](
            NonEmptyList.one(e.getLocalizedMessage -> repo)
          ),
        token => Ior.right(Set(Credentials(repo.realm, repo.host, "aws", token.unsafeValue)))
      )
    )

  def resolveCredentials[F[_]](
    f: CodeArtifactRepo => F[CodeArtifactAuthToken]
  )(repos: Vector[CodeArtifactRepo])(implicit
    F: ApplicativeError[F, Throwable],
    P: Parallel[F]
  ): F[Ior[NonEmptyList[(String, CodeArtifactRepo)], Set[Credentials]]] =
    NonEmptyVector
      .fromVector(repos)
      .fold(F.pure(Ior.right[NonEmptyList[(String, CodeArtifactRepo)], Set[Credentials]](Set.empty)))(nev =>
        nonEmptyResolveCredentials[F, NonEmptyVector](f)(nev)
      )

  def resolversToCodeArtifactRepos[F[_]](resolvers: F[Resolver])(implicit F: Foldable[F]): SortedSet[CodeArtifactRepo] =
    resolvers.foldMap {
      case resolver: MavenRepository =>
        CodeArtifactRepo
          .fromString(resolver.root)
          .fold(_ => SortedSet.empty[CodeArtifactRepo], value => SortedSet(value))
      case _ =>
        SortedSet.empty
    }
}
