## `@elide-tools/elide` [![Build status](https://badge.buildkite.com/7a69b0fadb7d08b691e96177f589971a7646217b1a8b4a269e.svg)](https://buildkite.com/bloomworks/elide)

Elide is a polyglot software application development framework. Bring the most road-tested toolchain on the planet to your development process, with [Bazel](https://bazel.build), [Protobuf](https://developers.google.com/protocol-buffers), [gRPC](https://grpc.io), [Soy](https://github.com/google/closure-templates), and more, all pre-integrated and tested out of the box. Early support is included for Java, Kotlin, Python, and NodeJS on the backend; Closure, TypeScript, or even Java on the frontend (transpiled to highly-optimized and type-checked JS); and Java, Kotlin, Swift, Objective-C and C/C++ for mobile applications.

Runtime libraries are also published to support easy development of _web applications_ (in Java and Python using Soy), and _mobile applications_ (in Java and Swift). The framework is also capable of leveraging [GraalVM](https://graalvm.org) to build your app into a [native application](https://www.graalvm.org/docs/reference-manual/native-image/) (where supported), alleviating the need entirely for a JVM or Python runtime in your app containers. Native apps also have the nice benefit of _way_ shorter startup times (try `30ms`, lol!), and, in some cases, significant latency and memory use improvements.

### Sample Apps

As part of the framework code, a few sample apps are included. Each of these is usable as its own project, simply by copying the files in that directory to another location on disk, and then running `bazel build //...` from inside the new directory. Each sample app demonstrates diverse functionality supported by Elide, including supported languages and use cases, and at least one holistic application:

- `rest_mvc` (Java): Example of RESTful servers with Micronaut.
- `soy_ssr` (Java): Server-side-rendered pages with Soy and Micronaut.
- `node_mvc` (TypeScript/JS): Server-side MVC app running in Node
- `todolist` (Java/Closure): Full holistic app example (see below for more)

### Todolist App ([Demo](https://todolist.apps.bloomworks.io))

The richest and best example app is *Todolist*, which is designed to be an example SaaS startup in a box. This includes a server app, a web app, mobile apps (for Android and iOS), and full-blown declarative infrastructure to support it all. The app includes the following components and features:
- **Devops toolkit**
  - Support for testing in each language, with coverage
  - Easy support for debugging the backend or frontend apps
  - Coverage reporting to [Codecov](https://codecov.io)
  - CI with [Buildkite](https://buildkite.com) and [GCB](https://cloud.google.com/cloud_build)
  - Support for Google Cloud's RBE/RCE tools for shared build caching
- **Hosting and serving via GKE/GCLB**
  - Seamless production deployment from CI
  - All-declarative infrastructure, from scratch
  - Anycast hosting (IPv4 and IPv6) with [Cloud CDN](https://cloud.google.com/cdn) support
  - Canary and blue/green deployment support
  - Rich error reporting and production debugging/logging
- **Server-side app written in Java and Micronaut**
  - Central schema via [Protobuf](https://developers.google.com/protocol-buffers)
  - SSR-based page serving with [Soy](https://github.com/google/closure-templates)
  - Schema-driven [gRPC](https://grpc.io) services with [seamless Firebase auth](https://firebase.google.com/docs/auth)
  - JSON/REST and Protobuf service invocation, via [Endpoints Service Proxy (ESP)](https://github.com/cloudendpoints/esp) and [Envoy](https://www.envoyproxy.io/)
  - Automatic schema-driven object storage and querying via [Firestore](https://firebase.google.com/docs/firestore)
  - Caching support via JSR-107 (JCache) and Redis
  - Packaging via Docker with intelligent layering
  - Native image support via [GraalVM](https://www.graalvm.org/)
  - Rich support for frontend security and performance features
    - Content Security Policy (CSP), with automatic JS nonces
    - Style rewriting for CSS classes and IDs via Soy
- **Client-side app written in Closure and J2CL**
  - Strict type-checking of all frontend code
  - Shared critical code between frontend and backend (via Java/[J2CL](https://github.com/google/j2cl)/[Elemental2](https://github.com/google/elemental2))
  - [Re-hydrated Hybrid S3R/CSR](https://developers.google.com/web/updates/2019/02/rendering-on-the-web#rehydration) with [Incremental DOM](https://github.com/google/incremental-dom)
  - Automatic, individual browser-tailored styles and scripts, built/served on the fly and aggressively cached
    - Scripts compiled and bundled via [Closure Compiler](https://developers.google.com/closure/compiler)
    - Styles compiled and bundled via [SCSS/SASS](https://sass-lang.com/), [GSS](https://github.com/google/closure-stylesheets), and [PostCSS](https://postcss.org/)
- **Android and iOS apps**
  - Generated workspace support via Bazel (Xcode/IntelliJ IDEA/Android Studio)
  - Secure and declarative signing key material, via [Cloud KMS](https://cloud.google.com/kms)
  - Seamless CI, CD, and release deployment (via App Store Connect and the Playstore)
  - Automatic service invocation wiring via [Dagger](https://dagger.dev/) and [SwiftGRPC](https://github.com/grpc/grpc-swift)

### Contributing

To build the framework, you'll need Bazel/Bazelisk and a standard toolchain of stuff (C/C++ compilers, Docker, JDK 11+, Git, and so on). Bazel should tell you what you need if it can't fetch it for you. Otherwise, file a bug on this repo if you have trouble building the framework itself.

Most development tasks on the framework involve the `Makefile`, which offers convenient invocation of common dev tools, including Bazelisk and `ibazel`. Running `make help` shows the commands supported, and generally returns something like this:
```
GUST Framework Tools:
bases                          Build base images and push them.
builder-image                  Build a new version of the CI builder image for Gust.
devtools                       Install local development dependencies.
distclean                      Clean targets, caches and dependencies.
docs                           Build documentation for the framework.
forceclean                     Clean everything, and sanitize the codebase (DANGEROUS).
help                           Show this help text.
release-images                 Pull, tag, and release Docker images.
report-coverage                Report coverage results to Codecov.
report-tests                   Report test results to Report.CI.
samples                        Build and push sample app images.
serve-coverage                 Serve the current coverage report (must generate first).
test                           Run all framework testsuites.
update-deps                    Re-seal and update all dependencies.
```

Obviously, decrypting keys or pushing release images may require permissions, however there should be no barrier to simple dev tasks like building the framework, building sample apps, and so on.

### Licensing

Below we specify licensing details for the Gust/Elide framework, including pointers to licenses for any dependent software. Gust/Elide itself is licensed under the Apache 2.0 License, which is enclosed in the `LICENSE.txt` file. Licenses for any dependent software (as required/applicable) are embedded in the `LICENSES/` directory, each within their own text file named for the software or framework.

- Gust/Elide: [Apache 2.0 License](https://github.com/sgammon/GUST/blob/master/LICENSE.txt)
- `rules_closure`: Apache 2.0 (Entry 1 in `LICENSES.txt`)
- gRPC Gateway: Roughly Apache 2.0 (Entry 2 in `LICENSES.txt`)
