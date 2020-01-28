package org.butcher.internals

import java.util.UUID

import cats.effect.IO
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import org.test.dynamo.createTable
import DynamoService._
import org.butcher.algebra.EncryptionResult

class DynamoServiceTest extends FunSuite with BeforeAndAfterAll with Matchers {
  val awsCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials("", ""))
  val db = AmazonDynamoDBAsyncClientBuilder.standard()
    .withCredentials(awsCredentialsProvider)
    .withEndpointConfiguration(
      new EndpointConfiguration(
        "http://dynamo:8000",
        "ap-southeast-2")).build()

  test("store") {
    val t = IO.fromFuture(IO(storeCipher("test",
      EncryptionResult("foo", UUID.randomUUID().toString, "bar")) .run(db)))
    t.unsafeRunSync.fold({_ => false should be(true)}, {_ => true should be(true)})
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    try {
      createTable("test", db)
    } catch {
      case e: Throwable => e.printStackTrace(System.err)
    }
  }

  override protected def afterAll(): Unit = {
    db.shutdown()
    super.afterAll()
  }
}
