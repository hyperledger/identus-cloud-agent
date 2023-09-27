import { Options } from 'k6/options';
import { connectionFlow } from '../common';
import {defaultScenarios, defaultThresholds} from "../../scenarios/default";
export let options: Options = {
  scenarios: {
    ...defaultScenarios
  },
  thresholds: {
    ...defaultThresholds
  }
}

export default() => {
  connectionFlow();
}
