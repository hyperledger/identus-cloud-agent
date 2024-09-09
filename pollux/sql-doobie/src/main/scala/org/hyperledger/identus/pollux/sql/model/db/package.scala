package org.hyperledger.identus.pollux.sql.model

import doobie._
import doobie.postgres._
import doobie.postgres.implicits._
import io.getquill.{MappedEncoding, PostgresJdbcContext, SnakeCase}
import io.getquill.doobie.DoobieContext
import org.hyperledger.identus.pollux.core.model.ResourceResolutionMethod
import org.hyperledger.identus.shared.models.WalletId
import org.postgresql.util.PGobject

import java.util.UUID

package object db {

  given MappedEncoding[WalletId, UUID] = MappedEncoding(_.toUUID)
  given MappedEncoding[UUID, WalletId] = MappedEncoding(WalletId.fromUUID)

  given mappedDecoderResourceResolutionMethod: MappedEncoding[ResourceResolutionMethod, String] =
    MappedEncoding[ResourceResolutionMethod, String](_.str)

  given mappedEncoderResourceResolutionMethod: MappedEncoding[String, ResourceResolutionMethod] =
    MappedEncoding[String, ResourceResolutionMethod] {
      case "did"  => ResourceResolutionMethod.DID
      case "http" => ResourceResolutionMethod.HTTP
      case other  => throw new IllegalArgumentException(s"Unknown ResourceResolutionMethod: $other")
    }

  trait PostgresEnumEncoders {
    this: DoobieContext.Postgres[_] =>

    given encoderResourceResolutionMethod: Encoder[ResourceResolutionMethod] = encoder[ResourceResolutionMethod](
      java.sql.Types.OTHER,
      (index: Index, value: ResourceResolutionMethod, row: PrepareRow) => {
        val pgObj = new PGobject()
        pgObj.setType("resolution_method_enum")
        pgObj.setValue(value.str)
        row.setObject(index, pgObj, java.sql.Types.OTHER)
      }
    )

    given decoderResourceResolutionMethod: Decoder[ResourceResolutionMethod] = decoder(row =>
      index =>
        row.getObject(index).toString match {
          case "did"  => ResourceResolutionMethod.DID
          case "http" => ResourceResolutionMethod.HTTP
        }
    )
  }

}
