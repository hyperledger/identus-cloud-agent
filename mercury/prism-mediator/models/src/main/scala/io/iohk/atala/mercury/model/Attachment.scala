package io.iohk.atala.mercury.model

import io.circe._

/** https://identity.foundation/didcomm-messaging/spec/#attachments
  *
  * Example:
  * {{{
  *      {"id":"765d7fda-1b7f-4325-a828-f0c5523a6c19",
  *        "data":{"jws":null,"hash":null,"json":{
  *          "ciphertext":"c1kzLNSIZAuRILhaJg7KY3rx95czEy6VVBFVCN002OWw4D7bLK-ZuPTaTVEvwhfxlwkkwP1xuG9R22XMYlzLUIou6hx3gyeWgsUZS6OZkvuQlRqtPLh1yEZap24WH80cH4_DpX-srxsic5n7cEluuUlC0xvF5th-TLOZcmfUYySBPoKLzrSBcNIZH0GPyeePlnmLhqB5pi--mX3M17DcTpN5miQyJUNaRNv8hj3lKsKiRtGUCL_dzbV4UGRFEZ-fF-LWZtZfNco3LoEwIpX5099sqzc9ZrFi3GbgAWdyJUe075A5h89FgMHYdqdOrGp8HSQCoj4pRTv-SQJJ16APFo-u7GZOVd0kLeJvMBCQhwXlGT4DUpaeMV_52wrPj2FA9jDyqnzpUFPsb_IH7poc-VFrV32NJ6GLhzgwwc3k3vU_s16bHB3GeB-7_GgCu6hT","protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6Ii1vY3RRYmFobFVYSF81aTQwOHRKN0Y3SmxHQU4yODY1Z1hvTHlHNTBNSHcifSwiYXB2IjoiYUptdEdPMHZIc3loQkpxVnRXU2dHNVN3YjMzVTJWdTRKTWhCS0ZiX0NHSSIsInNraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktYWdyZWVtZW50LTEiLCJhcHUiOiJaR2xrT21WNFlXMXdiR1U2WVd4cFkyVWphMlY1TFdGbmNtVmxiV1Z1ZEMweCIsInR5cCI6ImFwcGxpY2F0aW9uXC9kaWRjb21tLWVuY3J5cHRlZCtqc29uIiwiZW5jIjoiQTI1NkNCQy1IUzUxMiIsImFsZyI6IkVDREgtMVBVK0EyNTZLVyJ9",
  *          "recipients":[{
  *            "encrypted_key":"lWDvuQ37k6rotmnmOe7h1UF7Ao1RApL08aWmwjcijhlcH1_kvOvLYV8Dg2jXOZGsz2GsnM_W36JkLDFxM160g91ZsNAbz2rn",
  *            "header":{"kid":"did:example:bob#key-agreement-1"}
  *          }],"tag":"4pWPm37KdyRLZbWGIPJXqY9Mq55mKBHtSqBKUeHQHwc","iv":"THIgCu-Fq2aCiuwS-PcsfQ"
  *       }
  * }}}
  */
final case class Attachment(
    data: AttachmentData,
    id: String = java.util.UUID.randomUUID.toString(), // id: Option[String], OPTIONAL ????
    // description:Option[String],
    // media_type:Option[String],
    // format:Option[String],
    // lastmod_time:Option[String],
    // byte_count:  Option[Long],
) {
  // TODO user this
  // def asJson: Json =
  //   Json.obj("data" -> data, "id" -> Json.fromString(id))
}

type AttachmentData = JsonObject
