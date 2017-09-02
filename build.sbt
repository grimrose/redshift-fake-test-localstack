import Dependencies._

lazy val root = (project in file(".")).
  settings(
    name := "redshift-fake-test-localstack",
    organization := "com.github.grimrose",
    scalaVersion := "2.11.11",
    version := "0.1.0-SNAPSHOT",
    updateOptions := updateOptions.value.withCachedResolution(true),
    libraryDependencies ++= Seq(
      scalaTest % Test,
      // jdbc drivers
      redshiftDriver,
      redshiftFakeDriver,
      postgresDriver,
      hikariCP,
      // scalikejdbc
      scalikejdbc,
      logback,
      // utils
      awsSdkS3 % Test
    )
  )
