# Initial DDoS, WAF and Rate-limiting implementation

- Status: draft
- Deciders: David Poltorak, Milos Backonja, Kranium Mendoza
- Date: 2023-08-04
- Tags: DDoS, WAF, Rate-limiting

Technical Story:

## Context and Problem Statement

The Atala Prism platform requires a comprehensive solution to address the challenges of DDoS attacks, web application security, and rate limiting for APIs.

## Decision Drivers

- Initial implementation time-boxed for PI3
- Prefer k8s-native configuration and maintenance
- Prefer vendor-neutral solutions/on-prem compatibility
- Accessible price options

## Considered Options

- DDoS: Cloudflare DDoS
- DDoS: AWS Shield Advanced
- DDos: Fastly
- WAF: AWS WAF v2
- WAF: Cloudflare WAF
- WAF: OpenAppSec.io (APIsix plugin)
- Ratelimiting: AWS API Gateway
- Ratelimiting: apisix

## Decision Outcome

DDoS: Cloudflare DDoS
WAF: Cloudflare WAF
Rate-limiting: apisix

## Pros and Cons of the Options

### DDos: Cloudflare DDoS
Pros:
* Widely used and well understood
* Not tied to a cloud platform
* Available free-tier for developers to test
* k8s configuration via external-dns
Cons:
* Less PoPs than AWS, which could slow down performance

### DDos: AWS Shield Advanced
Pros:
* Already on the same platform we currently host our products
* Lots of documentation and examples
Cons:
* Shield is 'Proposed' as part of the aws-controllers-k8s project
* Requires ALB which is limited to only use ACM TLS certs
* No operator/controller support to provision ACM TLS certs

### DDos: Fastly
Pros:
- Widely used and well documented
- Not tied to a cloud platform
- Has a third-party k8s operator available
Cons:
- Free subscription has limited features
- Less transparent pricing options
- Less PoPs than AWS and Cloudflare, which could slow down performance

### WAF: AWS WAF v2
Pros:
* Already on the same platform we currently host our products
* Lots of documentation and examples
Cons:
* WAF is 'Proposed' as part of the aws-controllers-k8s project
* Requires ALB which is limited to only use ACM TLS certs
* No operator/controller support to provision ACM TLS certs

### WAF: Cloudflare WAF
Pros:
* Simpler as its part of cloudflare suite if used for DDoS
* Lots of documentation and examples
Cons:
* Will have to be configured with via terraform
* No operator/controller support to provision ACM TLS certs

### WAF: OpenAppSec.io
Pros:
- Open Source software managed by a security software company
- It can be run on-prem with k8s support
Cons:
- Appears to support kong-gateway and nginx-ingress but unclear to integrate with apisix (its own ingress)

### Ratelimiting: AWS API Gateway
Pros:
* Already on the same platform we currently host our products
* Lots of documentation and examples
Cons:
* Requires ALB which is limited to only use ACM TLS certs
* No operator/controller support to provision ACM TLS certs

### Ratelimiting: apisix
Pros:
- We already use this for ingress and has access to the agent-tokens
- It can be run on-prem
- k8s support
Cons:
- Bad, free subscription has limited features
- Bad, because it has less PoPs than AWS, which can slow down performance

## Links
- https://www.cloudflare.com/plans/
- https://aws.amazon.com/shield/pricing/
- https://aws-controllers-k8s.github.io/community/docs/community/releases/#project-stages
- https://docs.fastly.com/en/guides/account-types
- https://github.com/amazeeio/fastly-controller
- https://opensudo.org/why-cloudflare-is-far-better-than-fastly/
- https://www.openappsec.io/pricing
