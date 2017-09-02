import sbt._

object Dependencies {

  val redshiftDriverVersion = "1.2.1.1001"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"

  lazy val redshiftDriver = ("com.amazon.redshift" % "jdbc4" % redshiftDriverVersion)
    .from(s"https://s3.amazonaws.com/redshift-downloads/drivers/RedshiftJDBC42-$redshiftDriverVersion.jar")

  lazy val redshiftFakeDriver = "jp.ne.opt" %% "redshift-fake-driver" % "1.0.2"
  lazy val postgresDriver = "org.postgresql" % "postgresql" % "9.4.1211"

  lazy val hikariCP = "com.zaxxer" % "HikariCP" % "2.6.3"

  lazy val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % "3.0.2"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val awsSdkS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.186"

}
