// package io.iohk.atala

// import zio._

// import io.iohk.atala.mercury._
// import io.iohk.atala.mercury.given
// import io.circe.Printer
// import io.circe.syntax._
// import io.circe.Json._
// import io.circe.parser._
// import io.circe.JsonObject
// import io.circe.Encoder._
// import io.iohk.atala.mercury.model.Message
// import io.circe.generic.auto._, io.circe.syntax._
// import io.circe._, io.circe.parser._

// @main def AgentClientAlice() = {

//   val app = AgentPrograms.pickupMessageProgram
//     .provide(AgentService.alice, ZioHttpClient.layer)

//   Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

// }
