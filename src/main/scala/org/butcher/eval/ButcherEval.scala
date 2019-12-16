package org.butcher.eval

import cats.data.EitherT
import cats.effect.IO
import org.butcher.parser._
import com.roundeights.hasher.Implicits._
import org.butcher.ColumnReadable
import org.butcher.kms.CryptoDsl.TaglessCrypto
import org.butcher.parser.ButcherParser.nameSpecParser
import cats.implicits._

case class Butchered(column: String, value: String)

class ButcherEval(dsl: TaglessCrypto[IO]) {

  def evalWithHeader(spec: String, row: ColumnReadable[String]): List[Either[Throwable, Butchered]] = {
    val expressions = fastparse.parse(spec.trim, nameSpecParser(_))
    expressions.fold(
      onFailure = {(_, _, extra) => List(Left(new Throwable(extra.trace().longMsg)))},
      onSuccess = {
        case (es, _) =>
          eval(es, row)
      }
    )
  }

  def eval(ops: Seq[Expr], row: ColumnReadable[String]): List[Either[Throwable, Butchered]] = {
    ops.foldLeft(List.empty[Either[Throwable, Butchered]]) {
      case (acc, ColumnNamesMaskExpr(columns)) =>
        val masked = columns.map {
          c =>
            row.get(c).map(v => Butchered(c, v.sha256.hex))
        }
        acc ++ masked
      case (acc, ColumnNamesEncryptExpr(columns, keyId)) =>
        val encrypted = columns.map {
          c =>
            row.get(c).flatMap {
              v =>
                val io = for {
                  dk <- EitherT(dsl.generateKey(keyId))
                  ed <- EitherT(dsl.encrypt(v, dk))
                } yield ed

                io.value.unsafeRunSync().map(enc => Butchered(c, enc))
            }
        }
        acc ++ encrypted
      case (acc, _) => acc ++ List(Left(new Throwable("Unknown expression")))
    }
  }
}
