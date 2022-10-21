package io.iohk.atala.agent.walletapi.model

enum CommitmentPurpose {
  case Update extends CommitmentPurpose
  case Recovery extends CommitmentPurpose
}
