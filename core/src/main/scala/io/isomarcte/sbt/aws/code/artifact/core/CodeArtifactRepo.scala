package io.isomarcte.sbt.aws.code.artifact.core

import cats._
import cats.data._
import cats.syntax.all._
import io.circe._
import scala.util.matching.Regex

sealed abstract class CodeArtifactRepo extends Serializable {
  def domain: CodeArtifactDomain
  def owner: CodeArtifactDomainOwner
  def region: AWSRegion
  def name: CodeArtifactRepoName

  def mapDomain(f: CodeArtifactDomain => CodeArtifactDomain): CodeArtifactRepo
  def mapOwner(f: CodeArtifactDomainOwner => CodeArtifactDomainOwner): CodeArtifactRepo
  def mapRegion(f: AWSRegion => AWSRegion): CodeArtifactRepo
  def mapName(f: CodeArtifactRepoName => CodeArtifactRepoName): CodeArtifactRepo

  final def withDomain(value: CodeArtifactDomain): CodeArtifactRepo = mapDomain(_ => value)

  final def withOwner(value: CodeArtifactDomainOwner): CodeArtifactRepo = mapOwner(_ => value)

  final def withRegion(value: AWSRegion): CodeArtifactRepo = mapRegion(_ => value)

  final def withName(value: CodeArtifactRepoName): CodeArtifactRepo = mapName(_ => value)

  final def realm: String = s"${domain.value}/${name.value}"

  final def host: String = s"${domain.value}-${owner.value}.d.codeartifact.${region.value}.amazonaws.com"

  final def url: String = s"https://${host}/maven/${name.value}/"

  final override def toString: String =
    s"CodeArtifactRepo(domain = ${domain}, owner = ${owner}, region = ${region}, name = ${name})"
}

object CodeArtifactRepo {
  final private[this] case class CodeArtifactRepoImpl(
    override val domain: CodeArtifactDomain,
    override val owner: CodeArtifactDomainOwner,
    override val region: AWSRegion,
    override val name: CodeArtifactRepoName
  ) extends CodeArtifactRepo {
    override def mapDomain(f: CodeArtifactDomain => CodeArtifactDomain): CodeArtifactRepo =
      copy(domain = f(this.domain))

    override def mapOwner(f: CodeArtifactDomainOwner => CodeArtifactDomainOwner): CodeArtifactRepo =
      copy(owner = f(this.owner))

    override def mapRegion(f: AWSRegion => AWSRegion): CodeArtifactRepo = copy(region = f(this.region))

    override def mapName(f: CodeArtifactRepoName => CodeArtifactRepoName): CodeArtifactRepo = copy(name = f(this.name))
  }

  private[this] val regex: Regex =
    """http[s]?://([^-]+)-([^.]+)\.d\.codeartifact\.([^.]+)\.amazonaws\.com/maven/([^/]+)[/]?""".r

  def fromString(value: String): Either[String, CodeArtifactRepo] = {
    val m: java.util.regex.Matcher = regex.pattern.matcher(value.trim)

    def group(n: Int): Either[NonEmptyList[Throwable], String] =
      ApplicativeError[Either[Throwable, *], Throwable]
        .catchNonFatal {
          (m.group(n))
        }
        .leftMap(NonEmptyList.one)

    if (m.matches) {
      (group(1), group(2), group(3), group(4))
        .mapN { case (domain, owner, region, name) =>
          CodeArtifactRepo(
            CodeArtifactDomain(domain),
            CodeArtifactDomainOwner(owner),
            AWSRegion(region),
            CodeArtifactRepoName(name)
          )
        }
        .leftMap(errors =>
          s"""Errors when making CodeArtifactRepo: ${errors.map(_.getLocalizedMessage).toList.mkString(", ")}."""
        )
    } else {
      Left(s"Unable to construct CodeArtifactRepo from ${value}.")
    }
  }

  def unsafeFromString(value: String): CodeArtifactRepo =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  def apply(
    domain: CodeArtifactDomain,
    owner: CodeArtifactDomainOwner,
    region: AWSRegion,
    name: CodeArtifactRepoName
  ): CodeArtifactRepo = CodeArtifactRepoImpl(domain = domain, owner = owner, region = region, name = name)

  implicit val orderAndHashForCodeArtifactRepo: Hash[CodeArtifactRepo] with Order[CodeArtifactRepo] =
    new Hash[CodeArtifactRepo] with Order[CodeArtifactRepo] {
      final override def hash(x: CodeArtifactRepo): Int = x.hashCode

      final override def compare(x: CodeArtifactRepo, y: CodeArtifactRepo): Int =
        x.domain.compare(y.domain) match {
          case 0 =>
            x.owner.compare(y.owner) match {
              case 0 =>
                x.region.compare(y.region) match {
                  case 0 =>
                    x.name.compare(y.name)
                  case otherwise =>
                    otherwise
                }
              case otherwise =>
                otherwise
            }
          case otherwise =>
            otherwise
        }
    }

  implicit def stdLibOrdering: Ordering[CodeArtifactRepo] = orderAndHashForCodeArtifactRepo.toOrdering

  implicit val circeCodec: Codec[CodeArtifactRepo] =
    Codec.forProduct4("domain", "owner", "region", "name")(CodeArtifactRepo.apply)(value =>
      (value.domain, value.owner, value.region, value.name)
    )
}
