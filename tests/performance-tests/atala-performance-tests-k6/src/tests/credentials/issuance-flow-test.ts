import { group } from 'k6';
import { Options } from 'k6/options';
import { Issuer, Holder } from '../../actors';

export let options: Options = {
  stages: [
    { duration: '1m', target: 5 },
  ],
  thresholds: {
    http_req_failed: [{
      threshold: 'rate<=0.05',
      abortOnFail: true,
    }],
    http_req_duration: ['p(95)<=100'],
    checks: ['rate>=0.99'],
  },
};

const issuer = new Issuer();
const holder = new Holder();

// This is setup code. It runs once at the beginning of the test, regardless of the number of VUs.
export function setup() {

  group('Issuer publishes DID', function () {
    issuer.createAndPublishDid();
  });

  group('Holder creates unpublished DID', function () {
    holder.createUnpublishedDid();
  });

  return { issuerDid: issuer.did, holderDid: holder.did };
}

export default (data: { issuerDid: string; holderDid: string; }) => {

  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;
  holder.did = data.holderDid;

  group('Issuer connects with Holder', function () {
    issuer.createHolderConnection();
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
    issuer.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithIssuer();
  });

  group('Issuer creates credential offer for Holder', function () {
    issuer.createCredentialOffer();
  });
  
  group('Holder achieves and accepts credential offer from Issuer', function () {
    holder.waitAndAcceptCredentialOffer();
  });

  group('Issuer issues credential to Holder', function () {
    issuer.receiveCredentialRequest();
    issuer.issueCredential();
  });

  group('Holder receives credential from Issuer', function () {
    holder.receiveCredential();
  });

};
