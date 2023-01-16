## gRPC API ##

We use gRPC messages for both: interactions with Iris, and also to serialise messages which reside in the DLT.

`protocol` folder contains only definitions which are posted to the DLT and
basically define low-level protocol operations.

Files outside the `protocol` folder describe messages and services which are used in gRPC interface of Iris,
using protocol messages in their definitions.
