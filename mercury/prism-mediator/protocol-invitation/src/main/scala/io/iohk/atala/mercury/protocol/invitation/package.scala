package io.iohk.atala.mercury.protocol

package object invitation {

  /** provides new msg id
    * @return
    */
  def getNewMsgId: String = java.util.UUID.randomUUID().toString
}
