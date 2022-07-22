package io.isomarcte.sbt.aws.code.artifact.core

import cats._
import io.circe._

final case class AWSRegion(value: String) extends AnyVal

object AWSRegion {
  implicit val hashAndOrderForAWSRegion: Hash[AWSRegion] with Order[AWSRegion] =
    new Hash[AWSRegion] with Order[AWSRegion] {
      override def hash(x: AWSRegion): Int = x.hashCode

      override def compare(x: AWSRegion, y: AWSRegion): Int = x.value.compare(y.value)
    }

  implicit def stdLibOrdering: Ordering[AWSRegion] = hashAndOrderForAWSRegion.toOrdering

  implicit def circeCodec: Codec[AWSRegion] =
    Codec.from(Decoder[String].map(AWSRegion.apply), Encoder[String].contramap(_.value))
}
