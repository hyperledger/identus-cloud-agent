package creative.anoncreds

import jnr.ffi.byref.PointerByReference

object PrintErrorCode {

  def printIfError(errorCode: ErrorCode)(implicit api: AnonCreds): ErrorCode = {
    if (errorCode != ErrorCode.SUCCESS) {
      val err = new PointerByReference()
      val subErr = api.anoncreds_get_current_error(err)
      if (subErr != ErrorCode.SUCCESS) {
        println("Warning, function failed and getting current error failed.")
        println(s"Warning, function failed with error code $errorCode")
        println(s"Warning, get current error failed with $subErr")
      } else {
        val errMsg = err.getValue.getString(0)
        println(s"Warning, function failed with error code $errorCode")
        println(s"Warning, get current error is $errMsg")
      }
    }
    errorCode
  }
}
