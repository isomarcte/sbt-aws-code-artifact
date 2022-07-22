package io.isomarcte.sbt.aws.code.artifact.core

import cats._
import io.circe._

final case class CodeArtifactDomain(value: String) extends AnyVal

object CodeArtifactDomain {
  implicit val hashAndOrderForCodeArtifactDomain: Hash[CodeArtifactDomain] with Order[CodeArtifactDomain] =
    new Hash[CodeArtifactDomain] with Order[CodeArtifactDomain] {
      override def hash(x: CodeArtifactDomain): Int = x.hashCode

      override def compare(x: CodeArtifactDomain, y: CodeArtifactDomain): Int = x.value.compare(y.value)
    }

  implicit def stdLibOrdering: Ordering[CodeArtifactDomain] = hashAndOrderForCodeArtifactDomain.toOrdering

  implicit def circeCodec: Codec[CodeArtifactDomain] =
    Codec.from(Decoder[String].map(CodeArtifactDomain.apply), Encoder[String].contramap(_.value))
}
