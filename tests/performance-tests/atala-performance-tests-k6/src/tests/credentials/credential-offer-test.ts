import { group } from 'k6';
import { Options } from 'k6/options';
import { Issuer, Holder } from '../../actors';
import { Connection } from '@input-output-hk/prism-typescript-client';

// export let options: Options = {
//   stages: [
//     { duration: '1m', target: 5 },
//   ],
//   thresholds: {
//     http_req_failed: [{
//       threshold: 'rate<=0.05',
//       abortOnFail: true,
//     }],
//     http_req_duration: ['p(95)<=100'],
//     checks: ['rate>=0.99'],
//   },
// };

export let options: Options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 3,
      duration: "1s",
    },
  },
  thresholds: {
    'http_req_duration{group:::Issuer creates credential offer}': ['max >= 0'],
    'http_reqs{group:::Issuer creates credential offer}': ['count >= 0'],
    'group_duration{group:::Issuer creates credential offer}': ['max >= 0'],
  },
};

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
