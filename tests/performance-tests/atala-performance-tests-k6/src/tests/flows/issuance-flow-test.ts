import { group } from "k6";
import { Options } from "k6/options";
import { issuer, holder } from "../common";
import { CredentialSchemaResponse } from "@input-output-hk/prism-typescript-client";
import { defaultOptions } from "../../scenarios/default";
import merge from "ts-deepmerge";

export const localOptions: Options = {
  thresholds: {
    "group_duration{group:::Issuer connects with Holder}": ["avg < 15000"],
    "group_duration{group:::Issuer creates credential offer for Holder}": [
      "avg < 60000",
    ],
    "group_duration{group:::Issuer issues credential to Holder}": [
      "avg < 60000",
    ],
    "group_duration{group:::Holder receives credential from Issuer}": [
      "avg < 15000",
    ],
  },
};
export let options: Options = merge(localOptions, defaultOptions);

// This is setup code. It runs once at the beginning of the test, regardless of the number of VUs.
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
  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;
  issuer.schema = data.issuerSchema;
  holder.did = data.holderDid;

  group("Issuer connects with Holder", function () {
    issuer.createHolderConnection();
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
    issuer.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithIssuer();
  });

  group("Issuer creates credential offer for Holder", function () {
    issuer.createCredentialOffer();
    issuer.waitForCredentialOfferToBeSent();
  });

  group(
    "Holder achieves and accepts credential offer from Issuer",
    function () {
      holder.waitAndAcceptCredentialOffer(issuer.credential!.thid);
    }
  );

  group("Issuer issues credential to Holder", function () {
    issuer.receiveCredentialRequest();
    issuer.issueCredential();
    issuer.waitForCredentialToBeSent();
  });

  group("Holder receives credential from Issuer", function () {
    holder.receiveCredential();
  });
};
