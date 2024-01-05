
import http from 'k6/http';
import { check } from 'k6';
import { RefinedResponse, ResponseType, RequestBody } from 'k6/http';
import { Counter } from 'k6/metrics';

export let statusChangeTimeouts = new Counter('status_change_timeouts');
/**
 * HttpService provides convenience methods for making HTTP requests using the k6 library.
 *
 * - reduces boilerplate code
 * - adds basic HTTP status code checks
 * - adds API key header
 * - adds content-type header
 * - adds base URL to all requests
 */
export class HttpService {
  private baseUrl: string;
  private apiKey: string;
  private params: { headers: { [key: string]: string } };

  /**
   * Constructs a new HttpService instance with the specified base URL and API key.
   * @param baseUrl The base URL for the API.
   * @param apiKey The API key to use for authentication.
   */
  constructor(baseUrl: string, apiKey: string) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.params = {
      headers: {
        "content-type": "application/json",
        "Accept": "application/json",
        apikey: this.apiKey,
      },
    };
  }

  public toJson(response: RefinedResponse<ResponseType>): any {
    return JSON.parse(response.body as string)
  }

  /**
   * Performs an HTTP POST request to the specified endpoint with the provided payload.
   * @param endpoint The API endpoint to post to.
   * @param payload The payload to include in the request body.
   * @param expectedStatus The expected HTTP status code for the response.
   * @returns An object containing the response status code and body
   */
  public post(
    endpoint: string,
    payload: string | RequestBody | null | undefined = null,
    expectedStatus: number = 201
  ): RefinedResponse<ResponseType> {
    if (typeof payload !== "string") {
      payload = JSON.stringify(payload)
    }
    const res = http.post(
      `${this.baseUrl}/${endpoint}`,
      payload,
      this.params
    );
    check(res, {
      [`status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    });
    return res;
  }

  /**
   * Performs an HTTP GET request to the specified endpoint.
   * @param endpoint The API endpoint to get.
   * @param expectedStatus The expected HTTP status code for the response.
   * @returns An object containing the response status code and body
   */
  public get(endpoint: string, expectedStatus: number = 200): RefinedResponse<ResponseType> {
    const res = http.get(`${this.baseUrl}/${endpoint}`, this.params);
    check(res, {
      [`status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    });
    return res;
  }

  /**
   * Performs an HTTP GET request to the specified endpoint but fails the HTTP request if the maximum poll has been exceeded
   * Used for checking the status of an async process where if maxPoll is exceeded, the result is counted as a k6 failure
   * rather than an exception [which does not contribute to the failed iteration count]
   * The caller of this function is expected to manage the state of the interaction (it must track the current poll attempt)
   *
   * Even if the currentPoll exceeds maxPoll - the request is made anyway
   *
   * @param currentPoll An integer representing which poll attempt this is
   * @param maxPoll An integer representing the maximum number of polls that should be attempetd
   * @param endpoint The API endpoint to get.
   * @param pollAssertion An arrow function to check if a request satisfies a poll predicate (the poll is complete)
   * @param expectedStatus The expected HTTP status code for the response.
   * @returns{RefinedResponse<ResponseType | undefined>} An object containing the response status code and body
   */
  public getPoll(currentPoll: number, maxPoll: number, endpoint: string, pollAssertion: (response: RefinedResponse<ResponseType>) => boolean, expectedStatus: number = 200): RefinedResponse<ResponseType> {
    const res = this.get(endpoint, expectedStatus);
    check(res, {
      [`operation exceeded max polls`]: (r) => currentPoll <= maxPoll && !pollAssertion(r),
    });
    return res;
  }

  /**
   * Performs an HTTP PATCH request to the specified endpoint with the provided payload.
   * @param endpoint The API endpoint to patch.
   * @param payload The payload to include in the request body.
   * @param expectedStatus The expected HTTP status code for the response.
   * @returns An object containing the response status code and body
   */
  public patch(
    endpoint: string,
    payload: string | RequestBody | null | undefined = null,
    expectedStatus: number = 200
  ): RefinedResponse<ResponseType> {
    if (typeof payload !== "string") {
      payload = JSON.stringify(payload)
    }
    const res = http.patch(
      `${this.baseUrl}/${endpoint}`,
      payload,
      this.params
    );
    check(res, {
      [`status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    });
    return res;
  }

  /**
   * Performs an HTTP DELETE request to the specified endpoint.
   * @param endpoint The API endpoint to delete.
   * @param expectedStatus The expected HTTP status code for the response.
   * @returns An object containing the response status code and an empty object.
   */
  public del(endpoint: string, expectedStatus: number = 200): RefinedResponse<ResponseType> {
    const res = http.del(`${this.baseUrl} / ${endpoint}`, null, this.params);
    check(res, {
      [`status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    });
    return res;
  }



}
