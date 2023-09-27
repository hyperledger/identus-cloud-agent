import { group } from 'k6';
import { Options } from 'k6/options';
import { Issuer, Holder } from '../../actors';
import { Connection } from '@input-output-hk/prism-typescript-client';
import { defaultScenarios, defaultThresholds } from '../../scenarios/default';
export let options: Options = {
  scenarios: {
    ...defaultScenarios
  },
  thresholds: {
    ...defaultThresholds
  }
}
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

  return {
      issuerDid: issuer.did,
      holderDid: holder.did,
      connectionWithHolder: issuer.connectionWithHolder!,
      connectionWithIssuer: holder.connectionWithIssuer!
    };
}

export default (data: { issuerDid: string; holderDid: string; connectionWithHolder: Connection, connectionWithIssuer: Connection }) => {

  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;
  holder.did = data.holderDid;
  issuer.connectionWithHolder = data.connectionWithHolder;
  holder.connectionWithIssuer = data.connectionWithIssuer;

  group('Issuer creates credential offer', function () {
    issuer.createCredentialOffer();
  });
};
