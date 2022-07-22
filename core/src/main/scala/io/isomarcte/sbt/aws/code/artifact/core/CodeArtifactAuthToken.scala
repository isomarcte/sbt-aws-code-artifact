package io.isomarcte.sbt.aws.code.artifact.core

import cats._
import io.circe._

final case class CodeArtifactAuthToken(unsafeValue: String) extends AnyVal {
  final override def toString: String = "CodeArtifactAuthToken(unsafeValue = <REDACTED>)"
}

object CodeArtifactAuthToken {
  implicit val hashAndOrderForCodeArtifactAuthToken: Hash[CodeArtifactAuthToken] with Order[CodeArtifactAuthToken] =
    new Hash[CodeArtifactAuthToken] with Order[CodeArtifactAuthToken] {
      override def hash(x: CodeArtifactAuthToken): Int = x.hashCode

      override def compare(x: CodeArtifactAuthToken, y: CodeArtifactAuthToken): Int =
        x.unsafeValue.compare(y.unsafeValue)
    }

  implicit def stdLibOrdering: Ordering[CodeArtifactAuthToken] = hashAndOrderForCodeArtifactAuthToken.toOrdering

  implicit def circeCodec: Codec[CodeArtifactAuthToken] =
    Codec.from(Decoder[String].map(CodeArtifactAuthToken.apply), Encoder[String].contramap(_.unsafeValue))
}
