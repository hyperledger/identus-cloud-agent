import { Options } from 'k6/options';
import { Issuer } from '../../actors';
import {defaultScenarios, defaultThresholds} from "../../scenarios/default";
export let options: Options = {
    scenarios: {
        ...defaultScenarios
    },
    thresholds: {
        ...defaultThresholds
    }
}

const issuer = new Issuer();

export default () => {
    issuer.createUnpublishedDid();
};
