import { Options } from 'k6/options';
import { Issuer } from '../../actors';

export let options: Options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 3,
      duration: "1m",
    },
  },
  thresholds: {
    http_req_failed: [{
      threshold: 'rate==0',
      abortOnFail: true,
    }],
    http_req_duration: ['p(95)<=500'],
    checks: ['rate==1'],
  },
};

const issuer = new Issuer();

export default () => {
    issuer.createUnpublishedDid();
};
