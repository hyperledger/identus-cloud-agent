import { group } from 'k6';
import { Options } from 'k6/options';
import { Issuer } from '../../actors';

export let options: Options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 3,
      duration: "1s",
    },
  },
  thresholds: {
    'http_req_duration{group:::Issuer creates credential schema}': ['max >= 0'],
    'http_reqs{group:::Issuer creates credential schema}': ['count >= 0'],
    'group_duration{group:::Issuer creates credential schema}': ['max >= 0'],
    checks: ['rate==1'],
    http_req_duration: ['p(95)<=100'],
  },
};

export const issuer = new Issuer();

export function setup() {
  group('Issuer publishes DID', function () {
    issuer.createUnpublishedDid();
    issuer.publishDid();
  });

  return { 
    issuerDid: issuer.did,
  };
}

export default (data: { issuerDid: string; }) => {

  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;

  group('Issuer creates credential schema', function () {
    issuer.createCredentialSchema();
  });
};
