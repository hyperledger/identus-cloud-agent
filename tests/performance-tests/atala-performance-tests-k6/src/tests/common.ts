import { group } from "k6";
import { Holder, Issuer } from "../actors";

export const issuer = new Issuer();
export const holder = new Holder();

export function connectionFlow() {
  group('Issuer initiates connection with Holder', function () {
    issuer.createHolderConnection();
  });

  group('Holder accepts connection with Issuer', function () {
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
  });

  group('Issuer finalizes connection with Holder', function () {
    issuer.finalizeConnectionWithHolder();
  });

  group('Holder finalizes connection with Issuer', function () {
    holder.finalizeConnectionWithIssuer();
  });
}
