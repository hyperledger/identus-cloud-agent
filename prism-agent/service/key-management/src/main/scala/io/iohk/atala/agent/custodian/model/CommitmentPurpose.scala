package io.iohk.atala.agent.custodian.model

enum CommitmentPurpose {
  case Update extends CommitmentPurpose
  case Recovery extends CommitmentPurpose
}
