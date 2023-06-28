import { Options } from 'k6/options';
import { Issuer } from '../../actors';

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

export default () => {
    issuer.createUnpublishedDid();
    issuer.publishDid();
};
