package com.github.grimrose.sandbox

import java.util.UUID

import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import org.scalatest._
import org.slf4j.{ Logger, LoggerFactory }
import scalikejdbc._

import scala.collection.JavaConverters._

class RedshiftFakeSpec
    extends FlatSpec
    with Matchers
    with DiagrammedAssertions
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val endPointKey = "fake.awsS3Endpoint"

  private val bucketName = "sample-bucket"

  private var endpoint: String = _

  private var s3: AmazonS3 = _

  private val keyPrefix = "unloaded_t_sample_"

  override protected def beforeAll(): Unit = {
    endpoint = Option(System.getenv("LOCALSTACK_HOST"))
      .map(localStack => s"http://$localStack:4572/")
      .getOrElse("s3://")

    logger.debug(s"endpoint -> $endpoint")

    System.setProperty(endPointKey, endpoint)

    s3 = AmazonS3ClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")))
      .withEndpointConfiguration(new EndpointConfiguration(endpoint, null))
      .enablePathStyleAccess()
      .build()

    s3.createBucket(bucketName)
  }

  override protected def afterAll(): Unit = {
    ConnectionPool.closeAll()

    s3.listObjects(bucketName)
      .getObjectSummaries
      .asScala
      .foreach(summary => s3.deleteObject(bucketName, summary.getKey))

    System.clearProperty(endPointKey)
  }

  it should "be unloaded" in {
    // setup
    setUpConnectionPool()

    // exercise
    DB localTx { implicit session =>
      val s3Path = s"$endpoint$bucketName/$keyPrefix"

      //language=Redshift
      sql"DROP TABLE IF EXISTS t_sample".execute().apply()

      sql"""CREATE TABLE t_sample(
              created_at TIMESTAMP NOT NULL,
              creator_id VARCHAR(36) NOT NULL,
              PRIMARY KEY(creator_id)
            )
         """.execute().apply()

      sql"INSERT INTO t_sample(created_at, creator_id) VALUES (current_timestamp, {id})"
        .batchByName(
          Seq(
            Seq('id -> UUID.randomUUID().toString),
            Seq('id -> UUID.randomUUID().toString),
            Seq('id -> UUID.randomUUID().toString)
          ): _*
        )
        .apply()

      //language=Redshift
      val query = "SELECT * FROM t_sample"
      val authorization =
        "CREDENTIALS AS 'aws_access_key_id=AKIAXXXXXXXXXXXXXXX;aws_secret_access_key=YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY'"

      // 1.0.2ではaws_access_key_id, aws_secret_access_keyのキーに基づくアクセスコントロールのみ指定出来る
      val unload =
        s"""UNLOAD ('$query') TO '$s3Path'
            $authorization
            DELIMITER AS '\t'
          """.stripMargin

      SQL(unload).execute().apply()
    }

    // verify
    val keys = s3.listObjects(bucketName).getObjectSummaries.asScala.map(_.getKey)
    keys.foreach(logger.debug)

    assert(keys.forall(key => key.startsWith(keyPrefix)))
  }

  private def setUpConnectionPool(): Unit = {
    val postgres = Option(System.getenv("POSTGRES_HOST")).getOrElse("127.0.0.1")
    val port = Option(System.getenv("POSTGRES_PORT")).map(_.toInt).getOrElse(5432)

    val url = s"jdbc:postgresqlredshift://$postgres:$port/sample"
    val user = "redshift_user"
    val password = "redshift_pass"

    val driverClassName = "jp.ne.opt.redshiftfake.postgres.FakePostgresqlDriver"

    val config = new HikariConfig()
    config.setJdbcUrl(url)
    config.setUsername(user)
    config.setPassword(password)
    config.setDriverClassName(driverClassName)
    config.setMinimumIdle(1)
    config.setMaximumPoolSize(1) // 検証なので

    ConnectionPool.singleton(new DataSourceConnectionPool(new HikariDataSource(config)) {
      override def close(): Unit = dataSource.asInstanceOf[HikariDataSource].close()
    })
  }

}
