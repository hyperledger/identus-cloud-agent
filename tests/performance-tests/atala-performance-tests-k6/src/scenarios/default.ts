/*global __ENV*/

import { Options } from "k6/options";

export const defaultOptions: Options = {
  setupTimeout: '120s',
  scenarios: {
    load: {
      // Subject the system on test to an average amount of load akin to a production load
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "1m", target: 1000 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
        { duration: "1m", target: 1000 }, // stay at 100 users for 30 minutes
        { duration: "1m", target: 0 }, // ramp-down to 0 users
      ],
      tags: { scenario_label: __ENV.SCENARIO_LABEL || "defaultScenarioLabel" }, // add label for filtering in observability platform
    },
    // smoke: {
    //   // a simple test to ensure performance tests work and requests don't fail
    //   executor: "shared-iterations",
    //   vus: 1,
    //   iterations: 1,
    //   tags: { scenario_label: __ENV.SCENARIO_LABEL || "defaultScenarioLabel" }, // add label for filtering in observability platform
    // },
  },
  thresholds: {
    http_req_failed: [
      // fail if more than 1% of requests fail during smoke test
      {
        threshold: "rate<0.1",
        abortOnFail: false,
      },
    ],
    http_req_duration: [
      { threshold: "p(95)<2000", abortOnFail: false }, // 95% of requests should complete within 2 seconds, but don't fail tests
      // { threshold: "p(99)<5000", abortOnFail: false }, // 99% of requests should complete within 5 seconds, but don't fail tests
    ],
    checks: [{ threshold: "rate>0.9", abortOnFail: false }], // the rate of successful checks must be above 90%

  }
}
