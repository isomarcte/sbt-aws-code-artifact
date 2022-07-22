package io.isomarcte.sbt.aws.code.artifact.core

import cats._
import io.circe._

final case class CodeArtifactRepoName(value: String) extends AnyVal

object CodeArtifactRepoName {
  implicit val hashAndOrderForCodeArtifactRepoName: Hash[CodeArtifactRepoName] with Order[CodeArtifactRepoName] =
    new Hash[CodeArtifactRepoName] with Order[CodeArtifactRepoName] {
      override def hash(x: CodeArtifactRepoName): Int = x.hashCode

      override def compare(x: CodeArtifactRepoName, y: CodeArtifactRepoName): Int = x.value.compare(y.value)
    }

  implicit def stdLibOrdering: Ordering[CodeArtifactRepoName] = hashAndOrderForCodeArtifactRepoName.toOrdering

  implicit def circeCodec: Codec[CodeArtifactRepoName] =
    Codec.from(Decoder[String].map(CodeArtifactRepoName.apply), Encoder[String].contramap(_.value))
}
