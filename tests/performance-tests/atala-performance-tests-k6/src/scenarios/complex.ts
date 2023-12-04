// import { Options } from 'k6/options';
//
// import { Options } from "k6/options";
//
// export let options: Options = {
//     scenarios: {
//         smoke: {
//             executor: "constant-vus",
//             vus: 3,
//             duration: "10s",
//             tags: { scenario_label: __ENV.SCENARIO_LABEL || "defaultScenarioLabel" }, // add label for filtering in observability platform
//         },
//     },
//     thresholds: {
//         http_req_failed: [
//             // fail if any requests fail during smoke test
//             {
//                 threshold: "rate=0",
//                 abortOnFail: true,
//             },
//         ],
//         http_req_duration: [
//             // fail if any requests take longer than 500ms on smoke test
//             {
//                 threshold: "p(95)<=500",
//                 abortOnFail: true,
//             },
//         ],
//         checks: ["rate==1"], // fail if any checks fail (the checks are defined in test code which is executed)
//     },
// };
//
// export let options: Options = {
//     scenarios: {
//         // breakpoint: {
//         //   executor: 'ramping-arrival-rate',
//         //   preAllocatedVUs: 2000,
//         //   startRate: 10,
//         //   timeUnit: "10s",
//         //   stages: [
//         //     { duration: '1h', target: 2000 }, // just slowly ramp-up to a HUGE load
//         //   ]
//         // },
//         connectionFlowSmoke: {
//             executor: 'constant-vus',
//             vus: 3,
//             duration: "1s",
//         },
//         connectionFlowAverage: {
//             executor: 'ramping-arrival-rate',
//             preAllocatedVUs: 100,
//             stages: [
//                 { duration: '5m', target: 100 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
//                 { duration: '30m', target: 100 }, // stay at 100 users for 30 minutes
//                 { duration: '5m', target: 0 }, // ramp-down to 0 users
//             ]
//         },
//         // stress: {
//         //   executor: 'ramping-arrival-rate',
//         //   preAllocatedVUs: 100,
//         //   stages: [
//         //     { duration: '5m', target: 200 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
//         //     { duration: '30m', target: 200 }, // stay at 100 users for 30 minutes
//         //     { duration: '5m', target: 0 }, // ramp-down to 0 users
//         //   ]
//         // },
//         // soak: {
//         //   executor: 'ramping-arrival-rate',
//         //   preAllocatedVUs: 1000,
//         //   stages: [
//         //     { duration: '5m', target: 100 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
//         //     { duration: '8h', target: 100 }, // stay at 100 users for 8 hours!!!
//         //     { duration: '5m', target: 0 }, // ramp-down to 0 users
//         //   ]
//         // },
//         // spike: {
//         //   executor: 'ramping-arrival-rate',
//         //   preAllocatedVUs: 100,
//         //   stages: [
//         //     { duration: '2m', target: 2000 }, // fast ramp-up to a high point
//         //     // No plateau
//         //     { duration: '1m', target: 0 }, // quick ramp-down to 0 users
//         //   ]
//         // },
//     },
//     thresholds: {
//         http_req_failed: [{
//             threshold: 'rate==0',
//             abortOnFail: true,
//         }],
//         http_req_duration: ['p(95)<=500'],
//         checks: ['rate==1'],
//     },
// };