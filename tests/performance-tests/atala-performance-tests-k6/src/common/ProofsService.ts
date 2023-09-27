import { HttpService } from "./HttpService";
import { sleep } from "k6";
import { Connection, PresentationStatus } from "@input-output-hk/prism-typescript-client";
import { WAITING_LOOP_MAX_ITERATIONS, WAITING_LOOP_PAUSE_INTERVAL } from "./Config";
import vu from "k6/execution";

/**
 * A service class for managing proofs in the application.
 * Extends the HttpService class.
 */
export class ProofsService extends HttpService {

  /**
   * Requests a proof from a specific connection.
   * @param {Connection} connection - The connection object.
   * @returns {string} The ID of the requested presentation.
   */
  requestProof(connection: Connection): string {
    const payload = `
      {
          "description": "Proof request",
          "connectionId": "${connection.connectionId}",
          "options":{
              "challenge": "11c91493-01b3-4c4d-ac36-b336bab5bddf",
              "domain": "https://example-verifier.com"
          },
          "proofs":[
              {
                  "schemaId": "https://schema.org/${vu.vu.idInInstance}-${vu.vu.idInTest}-${vu.vu.iterationInScenario}",
                  "trustIssuers": [
                      "did:web:atalaprism.io/users/testUser"
                  ]
              }
          ]
      }`
    const res = this.post("present-proof/presentations", payload);
    return this.toJson(res).presentationId as string;
  }

  /**
   * Accepts a proof request and associates it with a credential proof ID.
   * @param {PresentationStatus} presentation - The presentation status object.
   * @param {string} credentialProofId - The credential proof ID.
   */
  acceptProofRequest(presentation: PresentationStatus, credentialProofId: string) {
    const payload = `
      {
        "action": "request-accept",
        "proofId": [
          "${credentialProofId}"
        ]
      }`
    const res = this.patch(`present-proof/presentations/${presentation.presentationId}`, payload);
    return this.toJson(res).presentationId  as string;
  }

  /**
   * Retrieves a specific presentation by ID.
   * @param {string} presentationId - The ID of the presentation.
   * @returns {PresentationStatus} The presentation status object.
   */
  getPresentation(presentationId: string): PresentationStatus {
    const res = this.get(`present-proof/presentations/${presentationId}`);
    return this.toJson(res) as unknown as PresentationStatus;
  }

  /**
   * Retrieves all presentations.
   * @returns {PresentationStatus[]} An array of presentation status objects.
   */
  getPresentations(thid: string): PresentationStatus[] {
    const res = this.get(`present-proof/presentations?thid=${thid}`);
    return this.toJson(res).contents as unknown as PresentationStatus[];
  }

  /**
   * Waits for a proof request to be received.
   * @returns {PresentationStatus} The received presentation status object.
   * @throws {Error} If the proof request is not received within the maximum iterations.
   */
  waitForProof(thid: string): PresentationStatus {
    let iterations = 0;
    let presentation: PresentationStatus | undefined;
    do {
      presentation = this.getPresentations(thid).find(
        r => r.thid === thid && r.status === "RequestReceived"
      );
      if (presentation) {
        return presentation;
      }
      sleep(WAITING_LOOP_PAUSE_INTERVAL);
      iterations++;
    } while (iterations < WAITING_LOOP_MAX_ITERATIONS);
    throw new Error(`Presentation with offerId=${vu.vu.idInTest} not achieved during the waiting loop`);
  }

  /**
   * Waits for a presentation to reach a specific state.
   * @param {string} presentationId - The ID of the presentation.
   * @param {string} requiredState - The required state.
   * @throws {Error} If the presentation state does not reach the required state within the maximum iterations.
   */
  waitForPresentationState(presentationId: string, requiredState: string) {
    let iterations = 0;
    let state: string;
    do {
      state = this.getPresentation(presentationId).status;
      sleep(WAITING_LOOP_PAUSE_INTERVAL);
      iterations++;
    } while (state !== requiredState && iterations < WAITING_LOOP_MAX_ITERATIONS);
    if (state !== requiredState) {
      throw new Error(`Presentation state is ${state}, required ${requiredState}`);
    }
  }

}
