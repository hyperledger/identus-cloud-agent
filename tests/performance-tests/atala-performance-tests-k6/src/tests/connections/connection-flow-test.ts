import { group } from 'k6';
import { Options } from 'k6/options';
import { Issuer, Holder } from '../../actors';
import { Trend } from 'k6/metrics';

const issuer = new Issuer();
const holder = new Holder();

const issuerConnectionWaitingTime = new Trend('issuerConnectionWaitingTime', true);
const holderConnectionWaitingTime = new Trend('holderConnectionWaitingTime', true);

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

export default () => {

  group('Issuer initiates connection with Holder', function () {
    issuer.createHolderConnection();
  });

  group('Holder accepts connection with Issuer', function () {
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
  });

  group('Issuer finalizes connection with Holder', function () {
    const start = Date.now();
    issuer.finalizeConnectionWithHolder();
    issuerConnectionWaitingTime.add(Date.now() - start);
  });
  
  group('Holder finalizes connection with Issuer', function () {
    const start = Date.now();
    holder.finalizeConnectionWithIssuer();
    holderConnectionWaitingTime.add(Date.now() - start);
  });

};
