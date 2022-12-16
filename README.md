<h1 align="center">Atala PRISM v2 - The modern SSI ecosystem.</h1>
<p align="center">
  <img src="docs/images/logos/atala-prism-logo.svg" alt="atala-prism-logo" width="120px" height="120px" />
  <br>
  <i> is an ecosystem and development platform for Self-Sovereign Identity applications
  </i>
  <br>
</p>
<p align="center">
  <a href="https://www.atalaprism.io">
    <strong>www.atalaprism.io</strong>
  </a>
  <br>
</p>
<p align="center">
  <a href="CONTRIBUTING.md">Contributing Guidelines</a> · <a href="https://blog.atalaprism.io/">Blog</a>
</p>
<hr>

## Documentation

* [OpenAPI docs](openapi)

## Running a single instance locally

Instructions for running the `building-block` stack locally can be found here: [Running locally](infrastructure/local/README.md)

## Running multiple instances locally

Instructions for running multiple instances of the `building-block` stack locally can be found here: [Running multiple locally](infrastructure/multi/README.md)

## Developing

Instructions for running the `building-block` stack for development purposes can be found here: [Developing locally](infrastructure/local/README.md)

## Exposing an agent to the internet

By default, running an instance of the `building-block` stack locally will give you an agent that is accessible only within your own local area network. No external agents or mediators will be able to connect to your agent without manually configuring access into your local area network. The configuration required is out of the scope of this repository as it depends greatly on your own network configuration and set up.

In order to make this easier, we suggest the use of a tunneling service such as [ngrok](https://ngrok.com/) to create an externally accessible and known service endpoint without having to configure your local network to expose the agent.

Please visit the ngrok site, sign-up and follow the guides for configuring an ngrok agent on your machine.

You will need to download the ngrok binary, install it and connect your account - this is all documented clearly on the ngrok [Getting Started Guide](https://dashboard.ngrok.com/get-started/setup)

The free tier of ngrok will provision a random domain every time you start the an ngrok tunnel. 

You can view the status of an active ngrok tunnel by browsing to the `Web Interface` address which is shwon in the console. By default, this is [http://localhost:4040](http://localhost:4040)

Included in the `Running a single instance locally` script is an option to specify that you are using ngrok and it will attempt to discover an active ngrok tunnel running on `localhost` and set the value as the service endpoint.

It does this by making a request to the locally running web interface administration console and using a JSON parsing tool call `jq` 

> Please ensure that you install both [ngrok](https://ngrok.com/) and [jq](https://stedolan.github.io/jq/)

> Important Note: Running ngrok and exposing an agent will allow access to the agents DIDComm endpoint from anywhere on the internet. It will also allow - with the default configuration of the API Gateway - connection to any endpoint without authentication. This includes configuration endpoints. Please be ware that the security of running locally is not configured to production standards and consideration should be given to running an agent in this manner. Before running using ngrok, it is advised that the API Gateway, APISIX - is cofigured with consumer authentication and enforcement of an `apikey` within the header of any request made [apart from the DIDComm endpoint]



## Contributing

Read through our [contributing guidelines][contributing] to learn about our submission process, coding rules and more.

<hr>

**Love Atala PRISM? Give our repo a star :star: :arrow_up:.**

[openapi]: docs/README.md
[contributing]: CONTRIBUTING.md
