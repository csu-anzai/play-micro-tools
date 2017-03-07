package microtools.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

import akka.util.ByteString

trait Hash {
  def update(data: ByteBuffer): Hash

  def update(data: ByteString): Hash =
    data.asByteBuffers.foldLeft(this)((hash, buffer) => hash.update(buffer))

  def update(data: String): Hash =
    update(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)))

  def update(data: Int): Hash = update(ByteBuffer.allocate(4).putInt(data))

  def update(data: Long): Hash = update(ByteBuffer.allocate(8).putLong(data))

  def raw: Array[Byte]

  def base64(): String = Base64.getEncoder.withoutPadding().encodeToString(raw)

  def safeBase64(): String = Base64.getUrlEncoder.withoutPadding().encodeToString(raw)
}

object Hash {
  class StdMessageDigest(algorithm: String) extends Hash {
    val md = MessageDigest.getInstance(algorithm)

    override def update(data: ByteBuffer): StdMessageDigest = {
      md.update(data)
      this
    }

    override def raw: Array[Byte] = md.digest()
  }

  def sha256(): Hash = new StdMessageDigest("SHA-256")
}
