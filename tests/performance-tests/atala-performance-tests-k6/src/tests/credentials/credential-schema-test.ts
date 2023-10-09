import { group } from "k6";
import { Options } from "k6/options";
import { Issuer } from "../../actors";
import { defaultOptions } from "../../scenarios/default";
import merge from "ts-deepmerge";

export const localOptions: Options = {
  thresholds: {
    'group_duration{group:::Issuer creates credential schema}': ['avg < 15000']
  }
}
export let options: Options = merge(localOptions, defaultOptions)
export const issuer = new Issuer();

export function setup() {
  group("Issuer publishes DID", function () {
    issuer.createUnpublishedDid();
    issuer.publishDid();
  });

  return {
    issuerDid: issuer.did,
  };
}

export default (data: { issuerDid: string }) => {
  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;

  group("Issuer creates credential schema", function () {
    issuer.createCredentialSchema();
  });
};
