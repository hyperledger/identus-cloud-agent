# DidComm tests

This integration tests leverage a modified version of the identus typescript
edge-agent that enable us to modify the didcomm requests.

This modified library is in the `tgz` file located in the root.

## Build

To speed up the execution time in github runner, there's a `build` script that
creates a bundled execution file in the `dist` folder which can be executed
using only the `node dist/suite.spec.js` command.

To build the dist file just execute `npm run build`.

The execution in github runner uses the `dist` version

## Usage

Before starting the tests you have to have the `cloud-agent` and `mediator` running

You can start by `cd docker` and running `./run.sh <cloud-agent-version>` script.

> It uses the ports 8080, 8090, 8091


### Environment variables

| variable         | description                         | default                               |
|------------------|-------------------------------------|---------------------------------------|
| CLOUD_AGENT_URL  | Url for cloud-agent                 | <http://localhost:8090>               |
| MEDIATOR_OOB_URL | Url for mediator oob invitation     | <http://localhost:8080/invitationOOB> |
| WEBHOOK_PORT     | Port for the local server listen to | 3000                                  |
| WEBHOOK_URL      | Url for webhook url                 | <http://host.docker.internal:3000>    |

### Running tests

Install the dependencies running `npm i`.

To run the entire suite during the development you can use `npm test`.

or

You can filter the test by name using `npx tsx --test --test-reporter spec "./test/<file-name>.ts"`
to run a single file.

## Implementing new scenarios

Add the test to the `test` folder with `.spec.ts` extension.

This test uses the [node test framework](https://nodejs.org/api/test.html) for
simplicity and [chai library](https://www.chaijs.com/) for assertions.

> Remember to run `npm run build` to generate the bundled file for the automation
