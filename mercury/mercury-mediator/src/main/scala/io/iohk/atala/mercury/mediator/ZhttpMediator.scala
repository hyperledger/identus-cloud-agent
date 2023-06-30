package io.iohk.atala.mercury.mediator

import zio._
import zio.http._
import zio.http.model._

import io.iohk.atala.mercury._
import io.iohk.atala.mercury.resolvers.MediatorDidComm
import scala.io.Source
import java.util.Base64

/** sbt "mediator/runMain io.iohk.atala.mercury.mediator.ZhttpMediator" */
object ZhttpMediator extends ZIOAppDefault {
  val header = "content-type" -> MediaTypes.contentTypeEncrypted

  // Create HTTP route
  val app: HttpApp[DidAgent & DidOps & MailStorage & ConnectionStorage, Throwable] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! if req.headersAsList.exists(h => (h.key == header._1) && h.value == header._2) =>
      req.body.asString
        .flatMap(data => MediatorProgram.program(data))
        .map(str => Response.text(str))
    case Method.GET -> !! / "api" / "openapi-spec.yaml" =>
      ZIO.succeed(
        Response.text(
          Source.fromResource("mercury-mediator-openapi.yaml").iter.mkString
        )
      )
    case req @ Method.GET -> !! / "oob_url" =>
      val serverUrl = s"http://locahost:${MediatorProgram.port}?_oob="
      InvitationPrograms.createInvitationV2().map(oob => Response.text(serverUrl + oob))

    case req @ Method.GET -> !! / "peer" / keyAgreementBase64 / keyAuthenticationBase64 / endpointBase64 => // REMOVE For debug purpose
      import io.iohk.atala.mercury.PeerDID
      import com.nimbusds.jose.jwk.OctetKeyPair

      // REMOVE Example for debug purpose
      // val keyAgreement = OctetKeyPair.parse("""{"kty":"OKP","d":"_4e_7Xn6TWV9ic_Fo187hGgsDi566sTbfI3yj167Gqc","crv":"X25519","x":"KQ-H8O6zyAtakmN1yc-Dztjy9RLhXNHHbyjoscsh_lQ"}""")
      // "eyJrdHkiOiJPS1AiLCJkIjoiXzRlXzdYbjZUV1Y5aWNfRm8xODdoR2dzRGk1NjZzVGJmSTN5ajE2N0dxYyIsImNydiI6IlgyNTUxOSIsIngiOiJLUS1IOE82enlBdGFrbU4xeWMtRHp0ank5UkxoWE5ISGJ5am9zY3NoX2xRIn0="
      // val keyAuthentication = OctetKeyPair.parse("""{"kty":"OKP","d":"ctDfZA_duF92Ypx3xsZgv2yCUdusd-3oy-9pBkp5Rgk","crv":"Ed25519","x":"Zjkryaft13epwbriH7TrJpgl3Y1vB1Gibnv2WqDskZk"}""")
      // "eyJrdHkiOiJPS1AiLCJkIjoiY3REZlpBX2R1RjkyWXB4M3hzWmd2MnlDVWR1c2QtM295LTlwQmtwNVJnayIsImNydiI6IkVkMjU1MTkiLCJ4IjoiWmprcnlhZnQxM2Vwd2JyaUg3VHJKcGdsM1kxdkIxR2libnYyV3FEc2taayJ9"
      // val endpoint = "http://localhost:9999"
      // aHR0cDovL2xvY2FsaG9zdDo5OTk5

      val keyAgreement = OctetKeyPair.parse(
        String(
          Base64
            .getUrlDecoder()
            .decode(keyAgreementBase64)
        )
      )
      val keyAuthentication = OctetKeyPair.parse(
        String(
          Base64
            .getUrlDecoder()
            .decode(keyAuthenticationBase64)
        )
      )

      val agentDID = for {
        peer <- ZIO.succeed(
          PeerDID.makePeerDid(
            jwkForKeyAgreement = keyAgreement,
            jwkForKeyAuthentication = keyAuthentication,
            serviceEndpoint = Some(
              String(
                Base64
                  .getUrlDecoder()
                  .decode(endpointBase64)
              )
            )
          )
        )
        _ <- Console.printLine(s"New PEER DID: ${peer.did}") *>
          Console.printLine(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
          Console.printLine(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
        ret <- ZIO.succeed(Response.text(peer.did.value.toString))
      } yield (ret)
      agentDID

    case req @ Method.GET -> !! / "resolve" / did => // REMOVE For debug purpose
      import org.didcommx.peerdid.VerificationMaterialFormatPeerDID
      def getDIDDocument = org.didcommx.peerdid.PeerDIDResolver
        .resolvePeerDID(did, VerificationMaterialFormatPeerDID.JWK)
      ZIO.succeed(Response.text(getDIDDocument))

    case req =>
      ZIO.succeed(
        Response.text(
          s"The request must be a POST to root with the Header $header"
        )
      )
  }

  val server = {
    val config = ServerConfig(address = new java.net.InetSocketAddress(8080))
    ServerConfig.live(config)(using Trace.empty) >>> Server.live
  }

  override val run = { MediatorProgram.startLogo *> Server.serve(app) }
    .provide(
      server ++ MediatorDidComm.peerDidMediator ++ DidCommX.liveLayer ++ MailStorage.layer ++ ConnectionStorage.layer
    )
}
