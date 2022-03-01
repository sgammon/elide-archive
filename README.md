## `@elide` (v2)

<hr />

_**Elide is beta software. Consider yourself warned...**_

<hr />

Elide is a polyglot software application development framework. Bring the most road-tested toolchain on the planet to your development process, with [Bazel](https://bazel.build), [Protobuf](https://developers.google.com/protocol-buffers), [gRPC](https://grpc.io), [Soy](https://github.com/google/closure-templates), and more, all pre-integrated and tested out of the box. Early support is included for Java, Kotlin, Python, and NodeJS on the backend; Closure, TypeScript, or even Java on the frontend (transpiled to highly-optimized and type-checked JS); and Java, Kotlin, Swift, Objective-C and C/C++ for mobile applications.

Runtime libraries are also published to support easy development of _web applications_ (in Java and Python using Soy), and _mobile applications_ (in Java and Swift). The framework is also capable of leveraging [GraalVM](https://graalvm.org) to build your app into a [native application](https://www.graalvm.org/docs/reference-manual/native-image/) (where supported), alleviating the need entirely for a JVM or Python runtime in your app containers. Native apps also have the nice benefit of _way_ shorter startup times (try `30ms`, lol!), and, in some cases, significant latency and memory use improvements.
