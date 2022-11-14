package io.iohk.atala

import zio._

import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zhttp.http.{Method, Headers}

import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.{given}
import io.circe.Printer
import io.circe.syntax._
import io.circe.Json._
import io.circe.parser._
import io.circe.JsonObject
import io.circe.Encoder._
import io.iohk.atala.mercury.model.Message
import io.circe.generic.auto._, io.circe.syntax._
import io.circe._, io.circe.parser._

@main def AgentClientAlice() = {

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = AgentPrograms.pickupMessageProgram.provide(env, AgentService.alice)

  Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
