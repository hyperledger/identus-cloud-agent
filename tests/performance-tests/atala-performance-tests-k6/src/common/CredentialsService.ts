import { sleep } from "k6";
import { HttpService } from "./HttpService";
import { WAITING_LOOP_MAX_ITERATIONS, WAITING_LOOP_PAUSE_INTERVAL } from "./Config";
import { IssueCredentialRecord, Connection } from "@input-output-hk/prism-typescript-client";
import vu from "k6/execution";

/**
 * A service class for managing credentials in the application.
 * Extends the HttpService class.
 */
export class CredentialsService extends HttpService {

  /**
   * Creates a credential offer for a specific issuing DID and connection.
   * @param {string} issuingDid - The issuing DID.
   * @param {Connection} connection - The connection object.
   * @returns {IssueCredentialRecord} The created credential offer record.
   */
  createCredentialOffer(issuingDid: string, connection: Connection): IssueCredentialRecord {
    const payload = `{
        "claims": { "offerId": "${vu.vu.idInInstance}-${vu.vu.idInTest}-${vu.vu.iterationInScenario}" },
        "issuingDID": "${issuingDid}",
        "connectionId": "${connection.connectionId}",
        "automaticIssuance": false
      }`;
    const res = this.post("issue-credentials/credential-offers", payload);
    return res.json() as unknown as IssueCredentialRecord;
  }

  /**
   * Retrieves a specific credential record by ID.
   * @param {IssueCredentialRecord} record - The credential record.
   * @returns {IssueCredentialRecord} The credential record.
   */
  getCredentialRecord(record: IssueCredentialRecord): IssueCredentialRecord {
    const res = this.get(`issue-credentials/records/${record.recordId}`);
    return res.json() as unknown as IssueCredentialRecord;
  }

  /**
   * Retrieves all credential records.
   * @returns {IssueCredentialRecord[]} An array of credential records.
   */
  getCredentialRecords(): IssueCredentialRecord[] {
    const res = this.get("issue-credentials/records");
    return res.json("contents") as unknown as IssueCredentialRecord[];
  }

  /**
   * Accepts a credential offer and associates it with a subject DID.
   * @param {IssueCredentialRecord} record - The credential record.
   * @param {string} subjectDid - The subject DID.
   * @returns {IssueCredentialRecord} The updated credential record.
   */
  acceptCredentialOffer(record: IssueCredentialRecord, subjectDid: string): IssueCredentialRecord {
    const payload = { subjectId: subjectDid };
    const res = this.post(`issue-credentials/records/${record.recordId}/accept-offer`, payload, 200);
    return res.json() as unknown as IssueCredentialRecord;
  }

  /**
   * Issues a credential for a specific credential record.
   * @param {IssueCredentialRecord} record - The credential record.
   * @returns {IssueCredentialRecord} The updated credential record.
   */
  issueCredential(record: IssueCredentialRecord): IssueCredentialRecord {
    const res = this.post(`issue-credentials/records/${record.recordId}/issue-credential`, null, 200);
    return res.json() as unknown as IssueCredentialRecord;
  }

  /**
   * Waits for a credential offer to be received.
   * @returns {IssueCredentialRecord} The received credential offer record.
   * @throws {Error} If the credential offer is not received within the maximum iterations.
   */
  waitForCredentialOffer(): IssueCredentialRecord {
    let iterations = 0;
    let record: IssueCredentialRecord | undefined;
    do {
      record = this.getCredentialRecords().find(
        r => r.claims["offerId"] === `${vu.vu.idInInstance}-${vu.vu.idInTest}-${vu.vu.iterationInScenario}`
          && r.protocolState === "OfferReceived");
      if (record) {
        return record;
      }
      sleep(WAITING_LOOP_PAUSE_INTERVAL);
      iterations++;
    } while (iterations < WAITING_LOOP_MAX_ITERATIONS);
    throw new Error(`Record with offerId=${vu.vu.idInTest} not achieved during the waiting loop`);
  }

  /**
   * Waits for a credential to reach a specific state.
   * @param {IssueCredentialRecord} credentialRecord - The credential record.
   * @param {string} state - The required state.
   * @throws {Error} If the credential state does not reach the required state within the maximum iterations.
   */
  waitForCredentialState(credentialRecord: IssueCredentialRecord, state: string) {
    let iterations = 0;
    let currentState;
    do {
      const response = this.getCredentialRecord(credentialRecord);
      currentState = response.protocolState;
      sleep(WAITING_LOOP_PAUSE_INTERVAL);
      iterations++;
    } while (currentState !== state && iterations < WAITING_LOOP_MAX_ITERATIONS);
    if (currentState !== state) {
      throw new Error(`Credential is not ${state} after the waiting loop`);
    }
    if (__ENV.DEBUG) console.log(`Credential state achieved: ${currentState}`);
  }

}
