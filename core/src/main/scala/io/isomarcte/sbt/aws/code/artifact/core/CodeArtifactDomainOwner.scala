package io.isomarcte.sbt.aws.code.artifact.core

import cats._
import io.circe._

final case class CodeArtifactDomainOwner(value: String) extends AnyVal

object CodeArtifactDomainOwner {
  implicit val hashAndOrderForCodeArtifactDomainOwner
    : Hash[CodeArtifactDomainOwner] with Order[CodeArtifactDomainOwner] =
    new Hash[CodeArtifactDomainOwner] with Order[CodeArtifactDomainOwner] {
      override def hash(x: CodeArtifactDomainOwner): Int = x.hashCode

      override def compare(x: CodeArtifactDomainOwner, y: CodeArtifactDomainOwner): Int = x.value.compare(y.value)
    }

  implicit def stdLibOrdering: Ordering[CodeArtifactDomainOwner] = hashAndOrderForCodeArtifactDomainOwner.toOrdering

  implicit def circeCodec: Codec[CodeArtifactDomainOwner] =
    Codec.from(Decoder[String].map(CodeArtifactDomainOwner.apply), Encoder[String].contramap(_.value))
}
