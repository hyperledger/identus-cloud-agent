import { Options } from "k6/options";
import { Issuer } from "../../actors";
import merge from "ts-deepmerge";
import { group } from "k6";
import { defaultOptions } from "../../scenarios/default";

export const localOptions: Options = {
  thresholds: {
    "group_duration{group:::Issuer create published DID}": ["avg < 30000"],
  },
};
export let options: Options = merge(localOptions, defaultOptions);

const issuer = new Issuer();

export default () => {
  group("Issuer create published DID", function () {
    issuer.createUnpublishedDid();
    issuer.publishDid();
  });
};
