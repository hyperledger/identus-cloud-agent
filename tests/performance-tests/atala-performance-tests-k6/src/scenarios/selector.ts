import {  Scenario, Threshold } from "k6/options";

const SCENARIO_TYPE = __ENV.SCENARIO_TYPE || 'smoke';  // An env variable defining the default scenario - if not provided, 'smoke' is used

export const defaultScenarios: { [name: string]: Scenario } = {
  smoke: {
    // a simple test to ensure performance tests work and requests don't fail
    executor: "constant-vus",
    vus: 3,
    duration: "10s",
    tags: { scenario_label: __ENV.SCENARIO_LABEL || "defaultScenarioLabel" }, // add label for filtering in observability platform
  },
};

export const defaultThresholds: { [name: string]: Threshold[] } = {
  http_req_failed: [
    // fail if any requests fail during smoke test
    {
      threshold: "rate=0",
      abortOnFail: true,
    },
  ],
  http_req_duration: [
    // fail if any requests take longer than 500ms on smoke test
    { threshold: "p(95)<200", abortOnFail: true }, // 95% of requests should complete within 200ms
    { threshold: "p(99)<500", abortOnFail: true }, // 99% of requests should complete within 500ms
  ],
  checks: ["rate==1"], // fail if any checks fail (the checks are defined in test code which is executed)
};
