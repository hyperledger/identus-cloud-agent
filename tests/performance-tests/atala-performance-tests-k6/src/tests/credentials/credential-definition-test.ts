import { group } from "k6";
import { Options } from "k6/options";
import { Issuer } from "../../actors";
import { defaultOptions } from "../../scenarios/default";
import merge from "ts-deepmerge";
import { CredentialSchemaResponse } from "@input-output-hk/prism-typescript-client";

export const localOptions: Options = {
  // Important to have this threshold to have a special line for this group in the report
  thresholds: {
    'group_duration{group:::Issuer creates credential definition}': ['avg > 0']
  }
}
export let options: Options = merge(localOptions, defaultOptions)

export const issuer = new Issuer();

export function setup() {
  group("Issuer publishes DID", function () {
    issuer.createUnpublishedDid();
    issuer.publishDid();
  });

  group("Issuer creates credential schema", function () {
    issuer.createCredentialSchema("anoncred");
  });

  return {
    issuerDid: issuer.did, schema: issuer.schema
  };
}

export default (data: { issuerDid: string, schema: CredentialSchemaResponse }) => {
  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;
  issuer.schema = data.schema;

  group("Issuer creates credential definition", function () {
    issuer.createCredentialDefinition();
  });
};
