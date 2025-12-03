import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.4.0"
  private val mongoVersion     = "2.11.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"             %% "crypto-json-play-30"        % "8.4.0",
    "javax.xml.bind"           % "jaxb-api"                   % "2.3.1",
    "org.typelevel"           %% "cats-core"                  % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion,
    "org.scalatest"           %% "scalatest"                  % "3.2.19",
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0",
    "org.scalacheck"          %% "scalacheck"                 % "1.19.0",
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.18.0"
  ).map(_ % "test")
}
