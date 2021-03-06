package org.butcher.internals.interpreters

import cats.effect.IO
import com.amazonaws.services.kms.AWSKMS
import javax.crypto.spec.SecretKeySpec
import org.butcher.OpResult
import org.butcher.algebra.{CryptoDsl, DataKey}
import org.butcher.internals.KMSService
import org.butcher.internals.KMSService.{decryptData, encryptWith}

private[butcher] class KMSCryptoIOInterpreter(kms: AWSKMS) extends CryptoDsl[IO] {
  override def encrypt(data: String, dk: DataKey): IO[OpResult[String]] = IO(encryptWith(data, dk))

  override def decryptKey(cipher: String): IO[OpResult[SecretKeySpec]] = IO(KMSService.decryptKey(cipher).run(kms))

  override def decrypt(key: SecretKeySpec, encryptedData: String): IO[OpResult[String]] = IO(decryptData(key, encryptedData))
}
