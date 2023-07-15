# Performance framework for Atala PRISM

- Status: accepted
- Deciders: Anton Baliasnikov, Shota Jolbordi, David Poltorak
- Date: 2023-07-14
- Tags: benchmarks, performance, k6, load testing

Technical Story: [PRISM Performance Management](https://input-output.atlassian.net/browse/ATL-4119)

## Context and Problem Statement

There are multiple great solutions today on the market for load testing. We need to choose one that corresponds to our needs, infrastructure, and price. The objective is to assess our requirements and explore the range of available load-testing solutions. Subsequently, we will provide a proposal recommending the load-testing framework that best suits our needs.

## Decision Drivers <!-- optional -->

What are our needs? Let’s try to sum up the required capabilities based on [RFC-0028](https://input-output.atlassian.net/wiki/spaces/ATB/pages/3819044870/RFC+0028+-+PI2+-+Performance+Testing+Guidance+Framework), we need:

1. Create required performance scenarios on various levels, such as endpoint level (e.g. get connections) and  flow level (calling endpoints one by one in order, e.g. issuing credentials flow)
2. Do checks for each request to make sure achieved statuses and response data are correct
3. Custom metrics support as we have custom scenarios connected
4. Protocols support: HTTP (rest API), WebSocket (if we support it in the future for Mediator), gRPC (if we need PRISM Node direct benchmarks on gRPC level)
5. Create the required load over time depending on the scenario and type of test, the more different things are supported - the better
6. Easy to understand, read and share reports
7. Something that does not require too much RAM/CPU resources on the host machine to run, so we can use our custom GitHub runners for performance testing and possible Cloud Execution
8. The fast learning curve for everybody to contribute
9. Good documentation and examples to be able to develop complex scenarios faster
10. cloud support for tests running in analysis in the future to make results visible and present them easily as per request
11. Support for GitHub actions to integrate into CI/CD
12. open source to be able to customize if required
13. Cheap or free

## Considered Options

What’s on the market
Here is the list of TOP10 load test frameworks currently available on the market:

* Apache JMeter
* LoadRunner
* PFLB Platform
* Gatling
* K6
* LoadNinja
* WebLOAD
* BlazeMeter
* NeoLoad
* Locust

We can read multiple papers about their comparison, for example, this one for reference, but we need to understand the difference between them all and which pros and cons they have, and why they were created.

There are 3 main classes of frameworks:

1. enterprise - huge enterprise solutions in clouds with advanced tools to generate all kinds of load testing scenarios targeted to be used by non-technical testing engineers
2. lightweight - small, highly optimized solutions, open-sourced, easily extendable
3. mixed - developer-friendly frameworks that are usually open-sourced with some free version providing cloud-paid plans, a kind of a medium between enterprise and lightweight

All frameworks are using different technologies and approaches: JVM-based, JS-based, GO-based, etc; GUI-oriented solutions to simplify tests development; cloud-only, etc.

Keeping in mind that we are not interested in enterprise solutions, because:

1. They are very expensive
2. We are smarter than they think we are (for example, we don’t need GUI simplifications and blocks to generate scenarios, we would like to use our coding power here)

We can conclude that it makes sense to analyze and compare the following load-testing frameworks:

1. Gatling
2. K6
3. Locust

## Decision Outcome

Native support for CI, Grafana, lots of output formats, custom metrics, distributed modes, amazing native Cloud integration, but most importantly the extensive documentation with tutorials and videos makes K6 the best choice for us.

## Pros and Cons of the Options <!-- optional -->

### K6

Strengths:

* Modern, JavaScript-based: K6 is built on modern web technologies and uses JavaScript as its scripting language, which makes it accessible to a wide range of developers.
* Cloud-native: K6 is designed to work seamlessly with cloud-based infrastructure, making it easy to scale tests and generate high loads.
* Open-source: K6 is fully open-source, with an active community and extensive documentation.

Weaknesses:

* Limited protocol support: K6 currently only supports HTTP, WebSocket, and gRPC protocols, which may not be ideal for testing more complex systems.
* Limited customization: While K6 is highly customizable, it does not allow for as much customization as Gatling or Locust.
* JavaScript knowledge required: Developers need to be familiar with JavaScript or TypeScript to use K6 effectively.

Pros:

* JavaScript-based, easy to start using for everyone
* Native integration with Grafana that we’re using in our infrastructure
* A nice Cloud solution that can be used inside our infrastructure
* Custom metrics support
* Tons of output formats
* Native CI/CD support for GitHub actions
* Extensive, easy-to-read docs with tutorials and youtube videos

Cons:

* Not as extendable as Gatling or Locust
* Quite an expensive Cloud solution
* New for us, some learning curve is expected


### Gatling

Strengths:

* High performance and scalability: Gatling can simulate thousands of virtual users with high performance and low resource usage.
* User-friendly DSL: Gatling uses a domain-specific language (DSL) that is easy to read and write, making it accessible to developers and testers with varying levels of experience.
* Comprehensive reporting: Gatling provides detailed and customizable HTML reports that make it easy to analyze test results.

Weaknesses:

* Java-based: Gatling requires Java to run, which may not be ideal for some developers or organizations.
* Steep learning curve: Although the DSL is user-friendly, a learning curve is still associated with mastering Gatling's features and functionality.
* Limited community support: Gatling has a smaller community than some other load-testing tools, which may make it harder to find answers to specific questions or issues.

Pros:

* We already have the performance setup on Gatling and used it as part of 1.4
* Nice concepts and solid base DSL
* Performance scenarios are written in Scala or Kotlin which gives us 2 advantages: re-use data models from our Scala libraries in our benchmarks if required, and we don’t need to learn any new language to use the framework (most of the team is Scala, Kotlin programmers)
* Extendable with plugins
* Custom Cloud solutions available

Cons:

* [subjective opinion writing the code] DSL is not that good, lots of boilerplates to achieve simple things, not intuitive
* Custom metrics are available only in Enterprise
* No native integration with Grafana that we use in our infrastructure
* Documentation is not that good, many examples are implicit, no good video tutorials
* No native CI/CD integration
* Supports a lot less of output formats than K6
* Distributed load generation is very complex, not natively integrated


### Locust

Strengths:

* Python-based: Locust is built on Python, which is a popular and widely used programming language.
* Simple syntax: Locust uses a clean and intuitive syntax that is easy to read and write, even for beginners.
* Highly customizable: Locust allows you to customize your test scenarios using Python code, which gives you more flexibility and control over your tests.

Weaknesses:

* Limited protocol support: Locust currently only supports HTTP, WebSockets, and MQTT protocols, which may not be ideal for testing more complex systems.
* Limited reporting: Locust provides basic reporting out of the box, but more advanced reporting features require third-party plugins.
* Python knowledge required: While Locust's syntax is simple, developers still need to be familiar with Python to use it effectively.

Pros:

* Python-based, very intuitive, and easy-to-write scenarios
* Already used in IO for benchmarking in other projects
* Very lightweight and powerful with capabilities for distributed load testing out of the box
* Nice docs and examples
* Highly and easily extendable

Cons:

* No Cloud solution is available
* New for us, some learning curve is expected, but easier
