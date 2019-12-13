package org.butcher.eval

import cats.effect.IO
import cats.implicits._
import com.amazonaws.util.Base64
import org.butcher.ColumnReadable
import org.butcher.kms.{CryptoDsl, DataKey}
import org.butcher.kms.CryptoDsl.TaglessCrypto
import org.butcher.parser.ButcherParser.nameSpecParser
import org.butcher.parser.UnknownExpr
import org.scalatest.{FunSuite, Matchers}
import org.butcher.Implicits._

class ButcherEvalTest extends FunSuite with Matchers {
  val b64EncodedPlainTextKey = "acZLXO+SWyeV95LYvUMExQtGeDHExNkAjvXbpbUEMK0="
  val b64EncodedCipherTextBlob = "AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA=="

  val dk = DataKey(Base64.decode(b64EncodedPlainTextKey), b64EncodedCipherTextBlob)

  lazy val crypto = new TaglessCrypto[IO](new CryptoDsl[IO] {
    override def generateKey(keyId: String): IO[Either[Throwable, DataKey]] = IO.pure(dk.asRight)

    override def encrypt(data: String, dk: DataKey): IO[Either[Throwable, String]] = IO.pure("foo".asRight)
  })

  val evaluator = new ButcherEval(crypto)

  test("eval") {
    val ml =
      s"""
         |column names in [driversLicence] then encrypt using kms key foo
         |column names in [firstName] then mask
         |""".stripMargin

    val expressions = fastparse.parse(ml.trim, nameSpecParser(_))
    val p = Map("firstName" -> "Satan", "driversLicence" -> "666")
    expressions.fold(
      onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
      onSuccess = {
        case (es, _) =>
          evaluator.eval(es, p).sequence.fold({t => println(t); false should be(true)}, {
            r =>
              r should be(
                List(Butchered("driversLicence","foo"),
                  Butchered("firstName","c50d310cc268c44b1f99bf0dd61ad8d575f225e52f27847b08b2433bd9b97ee8")))
          })
      }
    )
  }

  test("unknown expression") {
    val es = Seq(UnknownExpr())
    val p = Map("firstName" -> "satan", "driversLicence" -> "666")
    evaluator.eval(es, p).sequence.isLeft should be(true)
  }
}
