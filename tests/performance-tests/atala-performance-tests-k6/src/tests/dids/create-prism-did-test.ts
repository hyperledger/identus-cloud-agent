import { Options } from 'k6/options';
import { Issuer } from '../../actors';
import { defaultOptions } from "../../scenarios/default";
import { group } from "k6";
import merge from "ts-deepmerge";

export const localOptions: Options = {
  thresholds: {
    'group_duration{group:::Issuer create unpublished DID}': ['avg < 15000']
  }
}
export let options: Options = merge(localOptions, defaultOptions)

const issuer = new Issuer();

export default () => {
  group("Issuer create unpublished DID", function () {
    issuer.createUnpublishedDid();
  });
};
