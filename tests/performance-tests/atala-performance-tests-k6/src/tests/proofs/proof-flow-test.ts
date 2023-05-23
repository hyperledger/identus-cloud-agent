import { group } from 'k6';
import { Options } from 'k6/options';
import { Issuer, Holder, Verifier } from '../../actors';

export let options: Options = {
  vus: 1,
  iterations: 1
};

const issuer = new Issuer();
const holder = new Holder();
const verifier = new Verifier();

export function setup() {
  group('Issuer publishes DID', function () {
    issuer.createAndPublishDid();
  });

  group('Holder creates unpublished DID', function () {
    holder.createUnpublishedDid();
  });

  return { issuerDid: issuer.did, holderDid: holder.did };
}

export default (data: { issuerDid: string; holderDid: string; }) => {

  issuer.did = data.issuerDid;
  holder.did = data.holderDid;

  group('Holder connects with Issuer', function () {
    issuer.createHolderConnection();
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
    issuer.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithIssuer();
  });

  group('Issuer creates credential offer for Holder', function () {
    issuer.createCredentialOffer();
    holder.waitAndAcceptCredentialOffer();
    issuer.receiveCredentialRequest();
    issuer.issueCredential();
    holder.receiveCredential();
  });

  group('Holder connects with Verifier', function () {
    verifier.createHolderConnection();
    holder.acceptVerifierConnection(verifier.connectionWithHolder!.invitation);
    verifier.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithVerifier();
  });

  group('Verifier requests proof from Holder', function () {
    verifier.requestProof();
    holder.waitAndAcceptProofRequest();
    verifier.acknowledgeProof();
  });

};
