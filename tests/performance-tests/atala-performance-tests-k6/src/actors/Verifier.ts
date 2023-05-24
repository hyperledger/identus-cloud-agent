import { Connection } from "@input-output-hk/prism-typescript-client";
import { Actor } from "./Actor";
import { VERIFIER_AGENT_API_KEY, VERIFIER_AGENT_URL } from "../common/Config";

export class Verifier extends Actor {

  /**
   * The connection with the Holder.
   */
  connectionWithHolder: Connection | undefined;

  /**
   * Presentation ID.
   */
  presentationId: string | undefined;

  /**
   * Creates a new instance of Verifier.
   */
  constructor() {
    super(VERIFIER_AGENT_URL, VERIFIER_AGENT_API_KEY);
  }

  /**
   * Creates a connection with the holder.
   */
  createHolderConnection() {
    this.connectionWithHolder = this.connectionService.createConnection();
  }

  /**
   * Waits for the connection with the holder to be finalized.
   */
  finalizeConnectionWithHolder() {
    this.connectionService.waitForConnectionState(
      this.connectionWithHolder!,
      "ConnectionResponseSent"
    );
  }

  /**
   * Requests proof from the holder.
   */
  requestProof() {
    this.presentationId = this.proofsService.requestProof(this.connectionWithHolder!);
  }

  /**
   * Acknowledges the proof received from the holder.
   */
  acknowledgeProof() {
    this.proofsService.waitForPresentationState(this.presentationId!, "PresentationVerified");
  }
}
