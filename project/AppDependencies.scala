import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.15.0"
  private val hmrcMongoVersion = "1.1.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % hmrcMongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion,
    "org.scalatest"           %% "scalatest"                  % "3.2.15",
    "org.mockito"              % "mockito-core"               % "4.11.0",
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0",
    "org.scalacheck"          %% "scalacheck"                 % "1.17.0",
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.15.0",
    "com.vladsch.flexmark"     % "flexmark-all"               % "0.62.2"
  ).map(_ % "test, it")
}
