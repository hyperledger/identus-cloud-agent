/*global __ENV*/

import { Connection, ConnectionInvitation, ConnectionStateEnum } from "@input-output-hk/prism-typescript-client";
import { WAITING_LOOP_MAX_ITERATIONS, WAITING_LOOP_PAUSE_INTERVAL } from "./Config";
import { HttpService } from "./HttpService";
import { sleep } from "k6";

/**
 * A service class for managing connections in the application.
 * Extends the HttpService class.
 */
export class ConnectionService extends HttpService {
  
  /**
   * Retrieves all connections.
   * @returns {Connection[]} An array of connections.
   */
  getConnections(): Connection[] {
    const res = this.get("connections");
    const connections = res.json("contents") as unknown as Connection[];
    return connections;
  }

  /**
   * Retrieves a specific connection by ID.
   * @param {string} connectionId - The ID of the connection.
   * @returns {Connection} The connection object.
   */
  getConnection(connectionId: string): Connection {
    const res = this.get(`connections/${connectionId}`);
    const connection = this.toJson(res) as unknown as Connection;
    return connection;
  }

  /**
   * Creates a new connection.
   * @returns {Connection} The created connection object.
   */
  createConnection(): Connection {
    const payload = { label: "test" };
    const connection = this.toJson(this.post("connections", payload)) as unknown as Connection;
    return connection;
  }

  /**
   * Accepts a connection invitation.
   * @param {ConnectionInvitation} invitation - The connection invitation object.
   * @returns {Connection} The accepted connection object.
   */
  acceptConnectionInvitation(invitation: ConnectionInvitation): Connection {
    const payload = { invitation: this.invitationFromUrl(invitation.invitationUrl) };
    const res = this.post("connection-invitations", payload, 200);
    return this.toJson(res) as unknown as Connection;
  }

  /**
   * Waits for a connection to reach the required state.
   * @param {Connection} connection - The connection object.
   * @param {ConnectionStateEnum} requiredState - The required connection state.
   * @throws {Error} If the connection state does not reach the required state within the maximum iterations.
   */
  waitForConnectionState(connection: Connection, requiredState: ConnectionStateEnum) {
    let iterations = 0;
    let state: ConnectionStateEnum;
    do {
      state = this.getConnection(connection.connectionId).state;
      if (__ENV.DEBUG) console.log(`Connection state: ${state}, required: ${requiredState}`);
      sleep(WAITING_LOOP_PAUSE_INTERVAL);
      iterations++;
    } while (state !== requiredState && iterations < WAITING_LOOP_MAX_ITERATIONS);
    if (state !== requiredState) {
      throw new Error(`Connection state is ${state}, required ${requiredState}`);
    }
  }

  /**
   * Extracts the invitation code from a full invitation URL.
   * @param {string} fullUrl - The full invitation URL.
   * @returns {string} The invitation code.
   */
  invitationFromUrl(fullUrl: string) {
    return fullUrl.split("=")[1];
  }
}
