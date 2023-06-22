import uniffi.anoncreds._
import uniffi.anoncreds.CredentialDefinition
object Uniffy extends App {
  val prover = new Prover()
  val seceret = prover.createLinkSecret();

  println("Prover secret")
  println(seceret.getBigNumber)
}
