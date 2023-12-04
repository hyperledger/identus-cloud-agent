import { Options } from "k6/options";
export let options: Options = {
  scenarios: {
    load: {
      // Subject the system on test to an average amount of load akin to a production load
      executor: "ramping-arrival-rate",
      preAllocatedVUs: 100,
      stages: [
        { duration: "5m", target: 1000 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
        { duration: "30m", target: 1000 }, // stay at 100 users for 30 minutes
        { duration: "5m", target: 0 }, // ramp-down to 0 users
      ],
      tags: { scenario_label: __ENV.SCENARIO_LABEL || "defaultScenarioLabel" }, // add label for filtering in observability platform
    },
  },
  thresholds: {
    http_req_failed: [
      { threshold: "rate=0", abortOnFail: true }, // fail if any requests fail during smoke test
    ],
    http_req_duration: [
      { threshold: "p(95)<200", abortOnFail: true }, // 95% of requests should complete within 200ms
      { threshold: "p(99)<500", abortOnFail: true }, // 99% of requests should complete within 500ms
    ],
      iteration_duration: [""]
    checks: ["rate>0.95"], // At least 95% of the checks should pass
  },
};
\