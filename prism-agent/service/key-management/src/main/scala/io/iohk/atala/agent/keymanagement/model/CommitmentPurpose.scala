package io.iohk.atala.agent.keymanagement.model

enum CommitmentPurpose {
  case Update extends CommitmentPurpose
  case Recovery extends CommitmentPurpose
}
