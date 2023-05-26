import { Connection, IssueCredentialRecord } from "@input-output-hk/prism-typescript-client";
import { Actor } from "./Actor";
import { ISSUER_AGENT_API_KEY, ISSUER_AGENT_URL } from "../common/Config";

export class Issuer extends Actor {

  /**
   * The connection with the holder.
   */
  connectionWithHolder: Connection | undefined;

  /**
   * The connection with the verifier.
   */
  connectionWithVerifier: Connection | undefined;

  /**
   * The credential to be issued.
   */
  credential: IssueCredentialRecord | undefined;

  /**
   * The DID template used to create a DID for Issuer.
   * assertionMethod is the only purpose for the public key required for Issuer.
   */
  issuerDidTemplate: string = `{
        "documentTemplate": {
            "publicKeys": [
                {
                    "id": "issuanceKey",
                    "purpose": "assertionMethod"
                }
            ],
            "services": []
        }
    }`;

  /**
   * Creates a new instance of Issuer.
  */
  constructor() {
    super(ISSUER_AGENT_URL, ISSUER_AGENT_API_KEY);
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
   * Creates and publishes a DID.
   */
  createAndPublishDid() {
    const unpublishedDid = this.didService.createUnpublishedDid(this.issuerDidTemplate).longFormDid;
    this.did = this.didService.publishDid(unpublishedDid).didRef;
    this.didService.waitForDidState(unpublishedDid, "PUBLISHED");
  }

  /**
   * Creates a credential offer for the holder.
   */
  createCredentialOffer() {
    this.credential = this.credentialsService.createCredentialOffer(this.did!, this.connectionWithHolder!);
    this.credentialsService.waitForCredentialState(this.credential!, "OfferSent");
  }

  /**
   * Receives credential request from the holder.
   */
  receiveCredentialRequest() {
    this.credentialsService.waitForCredentialState(this.credential!, "RequestReceived");
  }

  /**
   * Issues the credential to the holder.
   */
  issueCredential() {
    this.credentialsService.issueCredential(this.credential!);
    this.credentialsService.waitForCredentialState(this.credential!, "CredentialSent");
  }
}
