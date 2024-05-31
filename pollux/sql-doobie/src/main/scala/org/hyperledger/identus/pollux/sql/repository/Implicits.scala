package org.hyperledger.identus.pollux.sql.repository

import doobie.util.{Get, Put}
import org.hyperledger.identus.castor.core.model.did.{CanonicalPrismDID, PrismDID}
import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.vc.jwt.StatusPurpose
import org.hyperledger.identus.shared.models.WalletId

given didCommIDGet: Get[DidCommID] = Get[String].map(DidCommID(_))
given didCommIDPut: Put[DidCommID] = Put[String].contramap(_.value)

given walletIdGet: Get[WalletId] = Get[String].map(WalletId.fromUUIDString)
given walletIdPut: Put[WalletId] = Put[String].contramap(_.toString)

given prismDIDGet: Get[CanonicalPrismDID] =
  Get[String].map(s => PrismDID.fromString(s).fold(e => throw RuntimeException(e), _.asCanonical))
given prismDIDPut: Put[CanonicalPrismDID] = Put[String].contramap(_.toString)

given statusPurposeGet: Get[StatusPurpose] = Get[String].map {
  case "Revocation" => StatusPurpose.Revocation
  case "Suspension" => StatusPurpose.Suspension
  case purpose      => throw RuntimeException(s"Invalid status purpose - $purpose")
}

given statusPurposePut: Put[StatusPurpose] = Put[String].contramap {
  case StatusPurpose.Revocation => StatusPurpose.Revocation.str
  case StatusPurpose.Suspension => StatusPurpose.Suspension.str
}
