/**
 * Maximum number of iterations for the waiting loop.
 * If not provided, the default value is 40.
 */
export const WAITING_LOOP_MAX_ITERATIONS = Number(__ENV.MY_USER_AGENT) || 500;

/**
 * Pause interval in seconds for each iteration of the waiting loop.
 * If not provided, the default value is 1 second.
 */
export const WAITING_LOOP_PAUSE_INTERVAL = Number(__ENV.WAITING_LOOP_PAUSE_INTERVAL) || 0.1;

/**
 * URL for the Issuer agent.
 * If not provided, the default value is "http://localhost:8080/prism-agent".
 */
export const ISSUER_AGENT_URL = __ENV.ISSUER_AGENT_URL || "http://localhost:8080/prism-agent";

/**
 * API key for the Issuer agent.
 * If not provided, the default value is an empty string.
 */
export const ISSUER_AGENT_API_KEY = __ENV.ISSUER_AGENT_API_KEY || "";

/**
 * URL for the Holder agent.
 * If not provided, the default value is "http://localhost:8090/prism-agent".
 */
export const HOLDER_AGENT_URL = __ENV.HOLDER_AGENT_URL || "http://localhost:8090/prism-agent";

/**
 * API key for the Holder agent.
 * If not provided, the default value is an empty string.
 */
export const HOLDER_AGENT_API_KEY = __ENV.HOLDER_AGENT_API_KEY || "";

/**
 * URL for the Verifier agent.
 * If not provided, the default value is "http://localhost:8100/prism-agent".
 */
export const VERIFIER_AGENT_URL = __ENV.VERIFIER_AGENT_URL || "http://localhost:8100/prism-agent";

/**
 * API key for the Verifier agent.
 * If not provided, the default value is an empty string.
 */
export const VERIFIER_AGENT_API_KEY = __ENV.VERIFIER_AGENT_API_KEY || "";
