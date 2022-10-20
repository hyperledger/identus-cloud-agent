package io.iohk.atala.iris.core.model.ledger

import io.circe.Json
import io.circe.{ACursor, Json}
import io.iohk.atala.iris.proto.dlt as proto
import io.iohk.atala.shared.utils.BytesOps

import scala.util.Try

case class TransactionMetadata(json: Json)

object TransactionMetadata {
  // Last 16 bits of 344977920845, which is the decimal representation of the concatenation of the hexadecimal values
  // (50 52 49 53 4d) of the word PRISM in ASCII.
  val METADATA_PRISM_INDEX = 21325

  private val VERSION_KEY = "v"
  private val CONTENT_KEY = "c"
  private val LEDGER_KEY = "l"
  // Prefix to denote that the following characters represent a string of bytes in hexadecimal format
  // (needed by Cardano Wallet)
  private val BYTE_STRING_PREFIX = "0x"
  // Maximum number of bytes that can be represented by a byte string (enforced by Cardano Node)
  private val BYTE_STRING_LIMIT = 64

  private val MAP_KEY = "k"
  private val MAP_VALUE = "v"
  private val MAP_TYPE = "map"
  private val LIST_TYPE = "list"
  private val INT_TYPE = "int"
  private val STRING_TYPE = "string"
  private val BYTES_TYPE = "bytes"

  // TODO add ledger here
  def fromTransactionMetadata(
      expectedLedger: Ledger,
      metadata: TransactionMetadata
  ): Option[proto.IrisBatch] = {
    val prismMetadata = metadata.json.hcursor
      .downField(METADATA_PRISM_INDEX.toString)

    for {
      _ <- prismMetadata
        .downField(VERSION_KEY)
        .focus
        .flatMap(_.asNumber)
        .flatMap(_.toInt)
        .find(_ == 2)

      _ <- prismMetadata
        .downField(LEDGER_KEY)
        .focus
        .flatMap(_.asString)
        .find(_ == expectedLedger.name)

      result <- fromTransactionMetadataV2(prismMetadata)
    } yield result
  }

  private def fromTransactionMetadataV2(
      prismMetadata: ACursor
  ): Option[proto.IrisBatch] = {
    val bytes = prismMetadata
      .downField(CONTENT_KEY)
      .focus
      .flatMap(_.asArray)
      .getOrElse(Vector[Json]())
      .flatMap(parseByteString)
      .toArray
    if (bytes.isEmpty) {
      // Either the content does not exist, is not the right type, or is truly empty
      None
    } else {
      proto.IrisBatch.validate(bytes).toOption
    }
  }

  private def parseByteString(byteString: Json): Array[Byte] = {
    byteString.asString
      .map(_.stripPrefix(BYTE_STRING_PREFIX))
      .map(hex => Try(BytesOps.hexToBytes(hex)).getOrElse(Array[Byte]()))
      .getOrElse(Array())
  }

  def toCardanoTransactionMetadata(
      ledger: Ledger,
      irisBatch: proto.IrisBatch
  ): TransactionMetadata = {
    // This definition aligns with the rules described here https://developers.cardano.org/docs/transaction-metadata/
    // After posting that data to the Cardano blockchain, it gets transformed to JSON
    TransactionMetadata(
      Json.obj(
        METADATA_PRISM_INDEX.toString -> Json.obj(
          MAP_TYPE -> Json.arr(
            Json.obj(
              MAP_KEY -> Json.obj(STRING_TYPE -> Json.fromString(VERSION_KEY)),
              MAP_VALUE -> Json.obj(INT_TYPE -> Json.fromInt(2))
            ),
            Json.obj(
              MAP_KEY -> Json.obj(STRING_TYPE -> Json.fromString(LEDGER_KEY)),
              MAP_VALUE -> Json.obj(STRING_TYPE -> Json.fromString(ledger.name))
            ),
            Json.obj(
              MAP_KEY -> Json.obj(STRING_TYPE -> Json.fromString(CONTENT_KEY)),
              MAP_VALUE -> Json.obj(
                LIST_TYPE -> Json.arr(
                  irisBatch.toByteArray
                    .grouped(BYTE_STRING_LIMIT)
                    .map(bytes =>
                      Json.obj(
                        BYTES_TYPE -> Json.fromString(
                          BytesOps.bytesToHex(bytes)
                        )
                      )
                    )
                    .toSeq: _*
                )
              )
            )
          )
        )
      )
    )
  }

  def toInmemoryTransactionMetadata(
      ledger: Ledger,
      irisBatch: proto.IrisBatch
  ): TransactionMetadata =
    TransactionMetadata(
      Json.obj(
        METADATA_PRISM_INDEX.toString -> Json.obj(
          VERSION_KEY -> Json.fromInt(2),
          LEDGER_KEY -> Json.fromString(ledger.name),
          CONTENT_KEY -> Json.arr(
            irisBatch.toByteArray
              .grouped(BYTE_STRING_LIMIT)
              .map(bytes => Json.fromString(BytesOps.bytesToHex(bytes)))
              .toSeq: _*
          )
        )
      )
    )

  def estimateTxMetadataSize(ledger: Ledger, irisBatch: proto.IrisBatch): Int = {
    toCardanoTransactionMetadata(ledger, irisBatch).json.noSpaces.length
  }
}
