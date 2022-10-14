package io.iohk.atala

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.Hashtable

object QRcode {

  def getQr(text: String) = {
    val width = 40
    val height = 40
    val qrParam = new Hashtable[EncodeHintType, Object]()
    qrParam.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L)
    qrParam.put(EncodeHintType.CHARACTER_SET, "utf-8")
    try {
      val bitMatrix: BitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, qrParam);
      toAscii(bitMatrix)
    } catch case e: WriterException => e.printStackTrace()
  }

  def toAscii(bitMatrix: BitMatrix) =
    val sb = new StringBuilder()
    for (rows <- 0 to bitMatrix.getHeight - 1)
      for (cols <- 0 to bitMatrix.getWidth - 1)
        if (!bitMatrix.get(rows, cols))
          sb.append("  ") // white
        else sb.append("██") // black
      sb.append("\n")
    sb.toString()

}

@main def QRcodeMain() = {
  val text = "https://localhost:8001/?_oob=asfyukfuhgkflajfl"
  System.out.println(QRcode.getQr(text))
}
