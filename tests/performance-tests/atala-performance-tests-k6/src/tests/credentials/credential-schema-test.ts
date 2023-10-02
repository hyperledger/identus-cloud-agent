import { group } from "k6";
import { Options } from "k6/options";
import { Issuer } from "../../actors";
import { defaultOptions } from "../../scenarios/default";

export let options: Options = defaultOptions
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
