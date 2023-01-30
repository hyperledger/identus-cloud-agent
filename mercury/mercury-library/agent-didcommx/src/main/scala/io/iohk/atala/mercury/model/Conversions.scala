package io.iohk.atala.mercury.model

import org.didcommx.didcomm.model.*
import org.didcommx.didcomm.message.MessageBuilder
import org.didcommx.didcomm.message.Attachment as XAttachment

import scala.jdk.CollectionConverters.*
import io.iohk.atala.mercury.model.*

import java.util.Random
import io.circe.*
import org.didcommx.didcomm.message.Attachment.Data

given Conversion[PackEncryptedResult, EncryptedMessage] with {
  def apply(msg: PackEncryptedResult): EncryptedMessage = EncryptedMessageImp(msg)
}

given Conversion[Message, org.didcommx.didcomm.message.Message] with {
  def apply(msg: Message): org.didcommx.didcomm.message.Message = {
    val attachmentsAux = msg.attachments.toSeq.flatten.map { e => e: XAttachment } // cast
    val aux = new MessageBuilder(
      msg.id,
      // msg.body.toMap.asJava,
      JsonUtilsForDidCommx.fromJsonToJavaMap(msg.body),
      msg.piuri
    )

    msg.createdTime.foreach(e => aux.createdTime(e))

    (msg.createdTime, msg.expiresTimePlus) match {
      case (Some(t), Some(delta)) => aux.expiresTime(t + delta)
      case _                      => // nothing
    }
    aux.attachments(attachmentsAux.toList.asJava)

    // .customHeader("return_route", "all")

    msg.from.foreach(did => aux.from(did.value))
    msg.to.foreach(did => aux.to(Seq(did.value).asJava))

    msg.ack.flatMap(_.headOption).foreach(str => aux.ack(str)) // NOTE: headOption becuase DidCommx only support one ack
    msg.thid.foreach(str => aux.thid(str))
    msg.pthid.foreach(str => aux.pthid(str))
    aux.build()
  }

}

def json2Map(json: Json): Any = json match {
  case e if e.isArray   => e.asArray.get.toList.map(j => json2Map(j)).asJava
  case e if e.isBoolean => e.asBoolean.get
  case e if e.isNumber  => e.asNumber.flatMap(_.toBigDecimal).get
  case e if e.isObject  => e.asObject.get.toMap.view.mapValues(json2Map).toMap.asJava
  case e if e.isString  => e.asString.get
  case e if e.isNull    => null
  case _                => null // Impossible case but Json cases are private in circe ...
}

def mapValueToJson(obj: java.lang.Object): Json = {
  obj match {
    case null                 => Json.Null
    case b: java.lang.Boolean => Json.fromBoolean(b)
    case i: java.lang.Integer => Json.fromInt(i)
    case d: java.lang.Double =>
      Json.fromDouble(d).getOrElse(Json.fromDouble(0d).get)
    case l: java.lang.Long   => Json.fromLong(l)
    case s: java.lang.String => Json.fromString(String.valueOf(s))
    case array: com.nimbusds.jose.shaded.json.JSONArray => {
      Json.fromValues(array.iterator().asScala.map(mapValueToJson).toList)
    }
    case joseObject: com.nimbusds.jose.shaded.json.JSONObject =>
      Json.fromJsonObject {
        JsonObject.fromMap(
          joseObject
            .asInstanceOf[java.util.Map[String, Object]]
            .asScala
            .toMap
            .view
            .mapValues(mapValueToJson)
            .toMap
        )
      }
    case any => {
      println("*****NotImplemented***" + any.getClass().getCanonicalName() + "**********") // FIXME
      ???
    }
  }
}

given Conversion[AttachmentDescriptor, XAttachment] with {
  def apply(attachment: AttachmentDescriptor): XAttachment = {
    val id = attachment.id

    val data =
      attachment.data match {
        case JsonData(d) => {
          val hack: Map[String, ?] = d.toMap.view.mapValues(json2Map).toMap
          val hack2 = Map[String, Any]("jws" -> null, "hash" -> null, "json" -> hack.asJava) // OMG
          val id = attachment.id
          XAttachment.Data.Companion.parse(hack2.asJava)
        }
        case LinkData(links, hash) => new XAttachment.Data.Links(links.asJava, hash, null)
        case Base64(d)             => new XAttachment.Data.Base64(d, null, null)
        case _                     => ??? // FIXME later attachment data of other types
      }

    new XAttachment.Builder(id, data).build()
  }
}

given Conversion[XAttachment, AttachmentDescriptor] with {
  def apply(attachment: XAttachment): AttachmentDescriptor = {
    val data: AttachmentData = attachment.getData().toJSONObject.asScala.toMap match {
      case e if e contains ("json") =>
        val aux = e("json")
        val x = aux.asInstanceOf[java.util.Map[String, Object]].asScala.toMap.view.mapValues(mapValueToJson)
        JsonData(JsonObject.fromMap(x.toMap))
      case e if e contains ("base64") =>
        val tmp = e("base64").asInstanceOf[String] // ...
        Base64(tmp)
      case e if e contains ("links") =>
        val aux = e("links")
        val list = aux.asInstanceOf[java.util.AbstractList[String]]
        val linksSeq = list.iterator().asScala.toSeq
        LinkData(linksSeq, hash = attachment.getData().getHash())
      case e if e contains ("jws") =>
        val aux = e("jws")
        println(aux.getClass().getCanonicalName()) // TODO
        ???
    }

    AttachmentDescriptor(
      id = attachment.getId(),
      media_type = Option(attachment.getMediaType()),
      data = data,
      filename = Option(attachment.getFilename()),
      lastmod_time = Option(attachment.getLastModTime()),
      byte_count = Option(attachment.getByteCount()),
      description = Option(attachment.getDescription()),
    )
  }
}

given Conversion[PackSignedResult, SignedMesage] with {
  def apply(msg: PackSignedResult): SignedMesage = SignedMessageImp(msg)
}

given Conversion[UnpackResult, UnpackMessage] with {
  def apply(msg: UnpackResult): UnpackMessage = UnpackMessageImp(msg)
}
