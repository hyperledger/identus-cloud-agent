import { sleep } from "k6";
import { HttpService } from "./HttpService";
import { ISSUER_AGENT_URL, WAITING_LOOP_MAX_ITERATIONS, WAITING_LOOP_PAUSE_INTERVAL } from "./Config";
import { IssueCredentialRecord, Connection, CredentialSchemaResponse } from "@input-output-hk/prism-typescript-client";
import { crypto } from "k6/experimental/webcrypto";


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
  createCredentialOffer(issuingDid: string, connection: Connection, schema: CredentialSchemaResponse): IssueCredentialRecord {
    const payload = `{
        "claims": {
          "emailAddress": "${crypto.randomUUID()}-@atala.io",
          "familyName": "Test",
          "dateOfIssuance": "${new Date()}",
          "drivingLicenseID": "Test",
          "drivingClass": 1
        },
        "schemaId": "${ISSUER_AGENT_URL.replace("localhost", "host.docker.internal")}/schema-registry/schemas/${schema.guid}",
        "issuingDID": "${issuingDid}",
        "connectionId": "${connection.connectionId}",
        "automaticIssuance": false
      }`;
    const res = this.post("issue-credentials/credential-offers", payload);
    return this.toJson(res) as unknown as IssueCredentialRecord;
  }

  createCredentialSchema(issuingDid: string): CredentialSchemaResponse {
    const payload = `
    {
      "name": "${crypto.randomUUID()}}",
      "version": "1.0.0",
      "description": "Simple credential schema for the driving licence verifiable credential.",
      "type": "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json",
      "schema": {
        "$id": "https://example.com/driving-license-1.0",
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "description": "Driving License",
        "type": "object",
        "properties": {
          "emailAddress": {
            "type": "string",
            "format": "email"
          },
          "givenName": {
            "type": "string"
          },
          "familyName": {
            "type": "string"
          },
          "dateOfIssuance": {
            "type": "string"
          },
          "drivingLicenseID": {
            "type": "string"
          },
          "drivingClass": {
            "type": "integer"
          }
        },
        "required": [
          "emailAddress",
          "familyName",
          "dateOfIssuance",
          "drivingLicenseID",
          "drivingClass"
        ],
        "additionalProperties": false
      },
      "tags": [
        "driving",
        "licence",
        "id"
      ],
      "author": "${issuingDid}"
    }
    `
    const res = this.post("schema-registry/schemas", payload);
    return this.toJson(res) as unknown as CredentialSchemaResponse;
  }

  /**
   * Retrieves a specific credential record by ID.
   * @param {IssueCredentialRecord} record - The credential record.
   * @returns {IssueCredentialRecord} The credential record.
   */
  getCredentialRecord(record: IssueCredentialRecord): IssueCredentialRecord {
    const res = this.get(`issue-credentials/records/${record.recordId}`);
    return this.toJson(res) as unknown as IssueCredentialRecord;
  }

  /**
   * Retrieves all credential records.
   * @returns {IssueCredentialRecord[]} An array of credential records.
   */
  getCredentialRecords(thid: string): IssueCredentialRecord[] {
    const res = this.get(`issue-credentials/records?thid=${thid}`);
    return this.toJson(res).contents as unknown as IssueCredentialRecord[];
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
    return this.toJson(res) as unknown as IssueCredentialRecord;
  }

  /**
   * Issues a credential for a specific credential record.
   * @param {IssueCredentialRecord} record - The credential record.
   * @returns {IssueCredentialRecord} The updated credential record.
   */
  issueCredential(record: IssueCredentialRecord): IssueCredentialRecord {
    const res = this.post(`issue-credentials/records/${record.recordId}/issue-credential`, null, 200);
    return this.toJson(res) as unknown as IssueCredentialRecord;
  }

  /**
   * Waits for a credential offer to be received.
   * @returns {IssueCredentialRecord} The received credential offer record.
   * @throws {Error} If the credential offer is not received within the maximum iterations.
   */
  waitForCredentialOffer(thid: string): IssueCredentialRecord {
    let iterations = 0;
    let record: IssueCredentialRecord | undefined;
    do {
      // console.log(`Waiting for credential offer with thid=${thid}`)
      record = this.getCredentialRecords(thid).find(
        r => r.thid === thid && r.protocolState === "OfferReceived"
      );
      if (record) {
        return record;
      }
      sleep(WAITING_LOOP_PAUSE_INTERVAL);
      iterations++;
    } while (iterations < WAITING_LOOP_MAX_ITERATIONS);
    throw new Error(`Record with thid=${thid} not achieved during the waiting loop`);
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
  }

}
