package io.iohk.atala.pollux.vc.jwt.revocation

import io.iohk.atala.pollux.vc.jwt.revocation.BitStringError.{DecodingError, EncodingError, IndexOutOfBounds}
import zio.{IO, UIO, ZIO}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util
import java.util.Base64
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

class BitString private (val bitSet: util.BitSet, val size: Int) {
  def setRevoked(index: Int, value: Boolean): IO[IndexOutOfBounds, Unit] =
    if (index >= size) ZIO.fail(IndexOutOfBounds(s"bitIndex >= $size: $index"))
    else ZIO.attempt(bitSet.set(index, value)).mapError(t => IndexOutOfBounds(t.getMessage))

  def isRevoked(index: Int): IO[IndexOutOfBounds, Boolean] =
    if (index >= size) ZIO.fail(IndexOutOfBounds(s"bitIndex >= $size: $index"))
    else ZIO.attempt(bitSet.get(index)).mapError(t => IndexOutOfBounds(t.getMessage))

  def revokedCount(): UIO[Int] = ZIO.succeed(bitSet.stream().count().toInt)

  def encoded: IO[EncodingError, String] = {
    for {
      bitSetByteArray <- ZIO.succeed(bitSet.toByteArray)
      paddingByteArray = new Array[Byte](((size + 7) / 8) - bitSetByteArray.length)
      baos = new ByteArrayOutputStream()
      _ <- (for {
        gzipOutputStream <- ZIO.attempt(new GZIPOutputStream(baos))
        _ <- ZIO.attempt(gzipOutputStream.write(bitSetByteArray))
        _ <- ZIO.attempt(gzipOutputStream.write(paddingByteArray))
        _ <- ZIO.attempt(gzipOutputStream.close())
      } yield ()).mapError(t => EncodingError(t.getMessage))
    } yield {
      Base64.getUrlEncoder.encodeToString(baos.toByteArray)
    }
  }
}

object BitString {
  val MIN_SL2021_SIZE: Int = 131072

  def getInstance(): IO[BitStringError, BitString] = getInstance(MIN_SL2021_SIZE)

  def getInstance(size: Int): IO[BitStringError, BitString] = {
    if (size % 8 != 0) ZIO.fail(BitStringError.InvalidSize("Bit string size should be a multiple of 8"))
    else ZIO.succeed(BitString(new util.BitSet(size), size))
  }

  def valueOf(b64Value: String): IO[DecodingError, BitString] = {
    for {
      ba <- ZIO.attempt(Base64.getUrlDecoder.decode(b64Value)).mapError(t => DecodingError(t.getMessage))
    } yield {
      val bais = new ByteArrayInputStream(ba)
      val gzipInputStream = new GZIPInputStream(bais)
      val byteArray = gzipInputStream.readAllBytes()
      BitString(util.BitSet.valueOf(byteArray), byteArray.length * 8)
    }
  }
}

sealed trait BitStringError

object BitStringError {
  final case class InvalidSize(message: String) extends BitStringError
  final case class EncodingError(message: String) extends BitStringError
  final case class DecodingError(message: String) extends BitStringError
  final case class IndexOutOfBounds(message: String) extends BitStringError
}
