package io.iohk.atala.mercury.protocol

import java.util.UUID

package object invitation {

  /** provides new msg id
    * @return
    */
  def getNewMsgId: String = UUID.randomUUID().toString
}
