package org

package object butcher {
  trait ColumnReadable[T] {
    def get(column: String): Either[Throwable, T]
  }

  class NamedLookup[T](m: Map[String, T]) extends ColumnReadable[T] {
    override def get(column: String): Either[Throwable, T] = m.get(column).toRight(new Throwable(s"Column $column not found"))
  }

  object implicits {
    implicit def makeMyMapColumnReadable(m: Map[String, String]): NamedLookup[String] = new NamedLookup(m)
  }
}
