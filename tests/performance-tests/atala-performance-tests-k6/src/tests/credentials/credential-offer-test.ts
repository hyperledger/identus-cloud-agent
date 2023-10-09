import { group } from 'k6';
import { Options } from 'k6/options';
import { Issuer, Holder } from '../../actors';
import { Connection, CredentialSchemaResponse } from '@input-output-hk/prism-typescript-client';
import { defaultOptions } from "../../scenarios/default";
import merge from "ts-deepmerge";

export const localOptions: Options = {
  thresholds: {
    'group_duration{group:::Issuer creates credential offer}': ['avg < 15000']
  }
}
export let options: Options = merge(localOptions, defaultOptions)
export const issuer = new Issuer();
export const holder = new Holder();

export function setup() {
  group('Issuer publishes DID', function () {
    issuer.createUnpublishedDid();
    issuer.publishDid();
  });

  group('Holder creates unpublished DID', function () {
    holder.createUnpublishedDid();
  });

  group('Issuer connects with Holder', function () {
    issuer.createHolderConnection();
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
    issuer.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithIssuer();
  });

  group("Issuer creates credential schema", function () {
    issuer.createCredentialSchema();
  });

  return {
      issuerDid: issuer.did,
      holderDid: holder.did,
      issuerSchema: issuer.schema,
      connectionWithHolder: issuer.connectionWithHolder!,
      connectionWithIssuer: holder.connectionWithIssuer!
    };
}

export default (data: { issuerDid: string; holderDid: string; issuerSchema: CredentialSchemaResponse, connectionWithHolder: Connection, connectionWithIssuer: Connection }) => {

  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;
  issuer.schema = data.issuerSchema
  holder.did = data.holderDid;
  issuer.connectionWithHolder = data.connectionWithHolder;
  holder.connectionWithIssuer = data.connectionWithIssuer;


  group('Issuer creates credential offer', function () {
    issuer.createCredentialOffer();
  });
};
