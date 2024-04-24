package org.hyperledger.identus.pollux.core.model

enum CredentialFormat:
  case JWT extends CredentialFormat
  case AnonCreds extends CredentialFormat

object CredentialFormat {
  def fromString(str: String) = str match
    case "JWT"       => Some(CredentialFormat.JWT)
    case "AnonCreds" => Some(CredentialFormat.AnonCreds)
    case _           => None
}
