import { group } from "k6";
import { Options } from "k6/options";
import { Issuer, Holder, Verifier } from "../../actors";
import { CredentialSchemaResponse } from "@input-output-hk/prism-typescript-client";
import { defaultOptions } from "../../scenarios/default";
import merge from "ts-deepmerge";

export const localOptions: Options = {
  thresholds: {
    "group_duration{group:::Holder connects with Issuer}": ["avg < 15000"],
    "group_duration{group:::Issuer creates credential offer for Holder}": [
      "avg < 120000",
    ],
    "group_duration{group:::Holder connects with Verifier}": ["avg < 15000"],
    "group_duration{group:::Verifier requests proof from Holder}": [
      "avg < 15000",
    ],
  },
};
export let options: Options = merge(localOptions, defaultOptions);

const issuer = new Issuer();
const holder = new Holder();
const verifier = new Verifier();

export function setup() {
  group("Issuer publishes DID", function () {
    issuer.createUnpublishedDid();
    issuer.publishDid();
  });

  group("Issuer creates credential schema", function () {
    issuer.createCredentialSchema();
  });

  group("Holder creates unpublished DID", function () {
    holder.createUnpublishedDid();
  });

  return {
    issuerDid: issuer.did,
    holderDid: holder.did,
    issuerSchema: issuer.schema,
  };
}

export default (data: {
  issuerDid: string;
  holderDid: string;
  issuerSchema: CredentialSchemaResponse;
}) => {
  issuer.did = data.issuerDid;
  issuer.schema = data.issuerSchema;
  holder.did = data.holderDid;

  group("Holder connects with Issuer", function () {
    issuer.createHolderConnection();
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
    issuer.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithIssuer();
  });

  group("Issuer creates credential offer for Holder", function () {
    issuer.createCredentialOffer();
    issuer.waitForCredentialOfferToBeSent();
    holder.waitAndAcceptCredentialOffer(issuer.credential!.thid);
    issuer.receiveCredentialRequest();
    issuer.issueCredential();
    issuer.waitForCredentialToBeSent();
    holder.receiveCredential();
  });

  group("Holder connects with Verifier", function () {
    verifier.createHolderConnection();
    holder.acceptVerifierConnection(verifier.connectionWithHolder!.invitation);
    verifier.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithVerifier();
  });

  group("Verifier requests proof from Holder", function () {
    verifier.requestProof();
    holder.waitAndAcceptProofRequest(verifier.presentation!.thid);
    verifier.acknowledgeProof();
  });
};
