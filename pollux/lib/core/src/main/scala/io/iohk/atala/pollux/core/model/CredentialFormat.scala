package io.iohk.atala.pollux.core.model

enum CredentialFormat:
  case JWT extends CredentialFormat
  case AnonCreds extends CredentialFormat
