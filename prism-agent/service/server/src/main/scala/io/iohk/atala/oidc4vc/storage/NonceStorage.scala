package io.iohk.atala.oidc4vc.storage

trait NonceStorage {
  def getNonce(nonceExpiresAt: Long): String
  def storeNonce(nonce: String): Unit
  def hasNonce(nonce: String): Boolean
  def removeNonce(nonce: String): Unit

}
