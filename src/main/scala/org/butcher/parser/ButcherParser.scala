package org.butcher.parser

import fastparse.SingleLineWhitespace._
import fastparse._
sealed trait Expr

sealed trait EncryptColumnsExpr extends Expr {
  def columns: Seq[String]
}

final case class UnknownExpr() extends Expr
final case class EncryptColumnsWithPKExpression(encryptColumns: Seq[String], pkColumns: Seq[String]) extends Expr

object ButcherParser {
  private def newline[_: P] = P( NoTrace(StringIn("\r\n", "\n")) )
  private def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
  private def commaSeparatedTokensParser[_: P]: P[Seq[String]] = P(tokenParser.!.rep(min = 1, sep = ","))

  private def encryptUsingLine[_: P]  = P(IgnoreCase("encrypt columns [") ~ commaSeparatedTokensParser ~ IgnoreCase("]"))
  private def pkLine[_: P]  = P(IgnoreCase("with primary key columns [") ~ commaSeparatedTokensParser ~ IgnoreCase("]"))

  def block[_: P]: P[EncryptColumnsWithPKExpression] = P(encryptUsingLine ~ newline.rep(1).? ~ pkLine ).map {
    case (encryptColumns, pkColumns) => EncryptColumnsWithPKExpression(encryptColumns, pkColumns)
  }
}
