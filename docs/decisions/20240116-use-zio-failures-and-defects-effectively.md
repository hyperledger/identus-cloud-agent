# Use ZIO Failures and Defects Effectively

## Context and Problem Statement

ZIO is a powerful and purely functional library for building scalable and resilient applications in Scala. However,
effectively handling errors within the context of ZIO presents challenges that must be addressed.

Within our software development projects utilising ZIO, the management and handling of errors have emerged as areas
requiring more clarity and strategy. The existing practices have shown limitations in terms of their efficiency and
comprehensiveness.

The key issues are:

1. **Lack of Consistent Error Handling Strategies**: There's inconsistency in error handling across different modules
   and components of our ZIO-based applications, making it challenging to maintain a unified approach.
2. **Understanding and Communicating Errors**: There's a need for a clearer method to categorise errors and communicate
   these effectively within the team and across various layers, facilitating quicker identifications and resolutions.
3. **Optimising Error Recovery Mechanisms**: While ZIO provides powerful abstractions for error recovery, there's a
   necessity to optimise these mechanisms to ensure graceful degradation and resilience in our applications.

This ADR aims to explore and define strategies for utilising ZIO's capabilities more effectively in handling errors. It
will outline decision drivers, available options, their pros and cons, and ultimately, the recommended approach to
enhance our error management practices with the ZIO framework.

Effective management of errors directly impacts the reliability, maintainability, and customer experience of our
applications.

By addressing the challenges of consistent error handling, we aim to enhance the stability of our products, ensuring
reduced downtime, clearer communication with customers through structured error messages, and quicker issue
resolution.

This not only improves the overall customer experience but also accelerates feature delivery as developers can focus
more on implementing new functionalities rather than troubleshooting ad-hoc errors. The resulting streamlined
development process contributes to cost reduction and optimised resource allocation.

In essence, these efforts are customer-centric, aiming to deliver a reliable, efficient, and customer-friendly service
interface that positively impacts the overall customer experience and product adoption.

## Decision Drivers

1. **Consistency and Standardization**: A consistent and standardised approach to error handling across different
   modules and components of our ZIO applications is crucial. This consistency will ease code readability, maintenance,
   and team collaboration.
2. **Robustness and Resilience**: A key driver is to harden our applications against failures by leveraging ZIO's
   powerful error recovery mechanisms. Enhancing the robustness of our applications will improve their resilience in
   adverse conditions.
3. **Traceability and Debugging**: Reducing debugging time and efforts associated with error resolution is another
   driving factor. An efficient error-handling strategy should enable traceability and quicker identification,
   communication, and resolution of errors.
4. **User Experience and Reliability**: Improving the quality and clarity of error messages reported to our users is a
   key driver. We aim to refine error messages to enable users to better understand what’s going on and respond to
   issues,
   thereby enhancing the user experience and the overall reliability of our applications.
5. **Alignment with Best Practices**: Aligning our error-handling strategies with industry best practices and leveraging
   the full potential of ZIO's error management features is a driver. Adhering to established standards can lead to more
   effective and maintainable code.

## Considered Options

### Option 1: Continue With The Current Error Handling Strategy

Continuing with the existing "so-so" error handling strategy currently in place within our ZIO applications without
significant modifications or improvements.

Pros:

- Maintains continuity with current practices, potentially requiring minimal adjustments and avoiding immediate
  disruptions to ongoing projects.

Cons:

- Inconsistencies in the code base across the components and services may persist, leading to potential challenges in
  maintenance and scalability.
- Engineers may spend time reinventing error-handling solutions rather than leveraging established best practices or
  frameworks.
- No significant improvement in terms of traceability and problem debugging, potentially hindering the resolution of
  issues and defects.

### Option 2: Leverage ZIO Failures And Defects Effectively

Adopting best practices for error handling within ZIO applications and effectively utilising ZIO Failures and Defects,
defining and implementing stricter guidelines and protocols for error handling.

Pros:

- Ensures adherence to established best practices, promoting code consistency, reliability, and scalability across
  various components and services.
- Reduces the need for developers to reinvent error-handling solutions, allowing them to leverage proven strategies and
  frameworks.
- Improves traceability and problem debugging by employing standardised error-handling practices, facilitating quicker
  issue identification and resolution.

Cons:

- Will require adjustments to current code and practices, necessitating time and effort for implementation.Actual impact
  and workload still needs to be identified.

## Decision Outcome

It has been decided to pursue Option 2: **Leverage ZIO Failures and Defects Effectively**. This decision aligns with our
commitment to enhancing the reliability, scalability, and maintainability of our applications.

While we acknowledge this option requires more refactoring, its long-term benefits in terms of code quality, developer
efficiency, and robust error management outweigh the associated refactoring efforts.

## Option 2 - Implementation Rules and Principles

### Case 1: When designing a component or service

#### Carefully Segregate Error Types

When designing a new component or service, the nature of anticipated errors should be carefully considered, and a clear
distinction between expected errors (i.e. ZIO Failures or domain-specific errors) and unexpected errors (i.e. ZIO
Defects) should be made.

This segregation should be done according to the principles outlined in
the [ZIO Types of Errors](https://zio.dev/reference/error-management/types) documentation section.
That is, carefully distinguishing between:

- **ZIO Failures** are:
    - The expected/recoverable errors (i.e. domain-specific errors).
    - Declared in the Error channel of the effect => ZIO[R, E, A].
    - Supposed to be handled by the caller to prevent call stack propagation.

- **ZIO Defects** are:
    - The unexpected/unrecoverable errors.
    - Not represented in the ZIO effect.
    - We do NOT expect the caller to handle them.
    - Propagated throughout the call stack until converted to a Failure or logged for traceability and debugging
      purposes by
      the uppermost layer.

#### Use ADT to Model ZIO Failures

Use **Algebraic Data Types** (ADTs) to model domain-specific errors as ZIO failures within the component/service
interface.

_Implementation tips:_

- **Use a sealed trait or abstract class** to represent the hierarchy of ZIO Failures, allowing for a well-defined set
  of error possibilities.
- **Ensure the sealed trait extends Throwable** to enable usage of the **ZIO#orDie** and **ZIO#refineOrDie** methods,
  allowing the caller to convert ZIO failures to defects conveniently.
- **Define specific error cases within the companion object** of the sealed trait. This practice prevents potential
  conflicts when importing errors with common names (e.g. RecordNotFoundError), allowing users to prefix them with the
  name of the parent sealed trait for better code clarity.

_Example:_

```scala
sealed trait DomainError extends Throwable

object DomainError {
  final case class BusinessLogicError(message: String) extends DomainError

  final case class DataValidationError(message: String) extends DomainError
}
```

#### Use Scala 3 Union Types to Be More Specific About ZIO Failure Types

Using the Scala 3 Union Types feature to declare the expected failures of a ZIO effect should be preferred over using
the broader and more generic top-level sealed trait. This allows for a more explicit and detailed definition of
potential failure scenarios and enhances error handling accuracy on the caller side.

This principle is outlined in
the [following section](https://zio.dev/reference/error-management/best-practices/union-types) of the ZIO Error
Management Best Practices documentation.

_Example:_

```scala
trait MyService {
  def myMethod(): ZIO[Any, BusinessLogicError | DataValidationError, Unit]
}
```

#### Don’t Type Unexpected Errors (i.e. ZIO Defects)

When we first discover typed errors, it may be tempting to put every error into the ZIO failure type parameter/channel.
That is a mistake because we can't recover from all types of errors. When we encounter unexpected errors we can't do
anything with, we should let the application die (i.e. the ZIO fiber). This is known as the “Let it Crash” principle,
and it is a good approach for all unexpected errors.

This principle is outlined in
the [following section](https://zio.dev/reference/error-management/best-practices/unexpected-errors) of the ZIO Error
Management Best Practices documentation.

### Case 2: When calling an existing component or service

#### Only Catch Failures You Effectively Handle

As a user of an existing component or service, you should exclusively catch failures that you are prepared to
effectively handle. Any unhandled failures should be transformed into defects and propagated through the call stack. You
should not expect callers of your component to handle lower-level failures that you do not handle.

The conversion of a ZIO failure into a ZIO defect can be achieved using **ZIO#orDie** and **ZIO#refineOrDie**.

#### Use Failure Wrappers To Prevent Failures Leakage From Lower Layers

When invoking lower-level components, refrain from directly exposing their failure types in your component or service
interface.

Using failures wrappers serves to encapsulate and abstract failure types originating from lower-level components or
services. It prevents the direct exposure of these lower-level failure types in the interface of higher-level components
or services, thus enhancing loose coupling and safeguarding against leakage of underlying implementation details to the
caller.

While using failure wrappers to shield callers from lower-level failures is beneficial for abstraction and loose
coupling, **it should not be the default failure-handling strategy**. Lower-level failures should primarily be managed
at the component implementation level ensuring that the component handles and appropriately resolves and recovers them.

Unhandled failures within the component's boundaries should preferably be transformed into defects rather than
propagated upward.

#### Do not reflexively log errors

Avoid catching a ZIO failure or defect solely for the purpose of logging it. Instead, consider allowing the error to
propagate through the call stack. It's preferable to assume that the uppermost layer, commonly known as **'the end of
the world' will handle the logging** of those errors. This practice promotes a centralised and consistent logging
approach for better traceability and debugging.

This principle is outlined in
the [following section](https://zio.dev/reference/error-management/best-practices/logging-errors) of the ZIO Error
Management Best Practices documentation.

#### Adopt The “Let it Crash” Principle For ZIO Defects

Adopt the “Let it Crash” principle for ZIO defects. Let them bubble up the call stack and crash the current ZIO fibre.
They will be handled/recovered at a higher level or logged appropriately “at the end of the world” by the uppermost
layer.