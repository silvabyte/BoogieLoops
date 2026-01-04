# BoogieLoops: Type-Safe JSON Schema Ecosystem for Scala 3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/boogieloops)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

An ecosystem of libraries for JSON Schema generation, HTTP validation & Automatic OpenAPI Spec Generation, and LLM integration in Scala 3. Each module can be used independently or together for a complete solution.

## Installation

Choose the modules you need for your project:

### Mill

```scala
// Just schema validation
mvn"dev.boogieloop::schema:0.6.0"

// Add HTTP validation for Cask web framework
mvn"dev.boogieloop::schema:0.6.0"
mvn"dev.boogieloop::web:0.6.0"

// Full ecosystem with LLM agent support
mvn"dev.boogieloop::schema:0.6.0"
mvn"dev.boogieloop::web:0.6.0"
mvn"dev.boogieloop::ai:0.6.0"
```

### SBT

```scala
// Just schema validation
libraryDependencies += "dev.boogieloop" %% "schema" % "0.6.0"

// Add HTTP validation for Cask web framework
libraryDependencies ++= Seq(
  "dev.boogieloop" %% "schema" % "0.6.0",
  "dev.boogieloop" %% "web" % "0.6.0"
)

// Full ecosystem with LLM agent support
libraryDependencies ++= Seq(
  "dev.boogieloop" %% "schema" % "0.6.0",
  "dev.boogieloop" %% "web" % "0.6.0",
  "dev.boogieloop" %% "ai" % "0.6.0"
)
```

## The Ecosystem

- **schema** (core): Type-safe JSON Schema derivation and validation
- **web**: Schema-first HTTP validation + OpenAPI for Cask
- **ai**: Type-safe LLM agents with structured output

## Quick Example

```scala
import boogieloops.schema.bl
import boogieloops.schema.derivation.Schema

// Manual schema definition
val userSchema = bl.Object(
  "name" -> bl.String(),
  "age" -> bl.Integer(minimum = Some(0))
)

// Or derive from case class
case class User(name: String, age: Int) derives Schema
```

## Documentation

- Getting Started: [docs/README.md](./docs/README.md)
- Zero to App: [docs/zero-to-app.md](./docs/zero-to-app.md)
- Schema (core): [docs/schema.md](./docs/schema.md)
- Web: [docs/web.md](./docs/web.md)
- AI: [docs/ai.md](./docs/ai.md)
- Concepts: [docs/concepts.md](./docs/concepts.md)
- Troubleshooting: [docs/troubleshooting.md](./docs/troubleshooting.md)
- TypeScript SDK: [docs/typescript-sdk.md](./docs/typescript-sdk.md)

## Running Examples & Commands

- See all commands: `make help`
- Run examples: `make schema`, `make web`, `make ai`
- AI examples require `OPENAI_API_KEY` (or a local LLM endpoint)
- Test everything: `make test` (override `MODULE=schema|web|ai`)

AI Disclaimer: This project uses AI assistance for documentation creation as well as code generation for monotonous tasks. All architecture, design and more interesting code creation is done by a [human](https://x.com/MatSilva)

## License

MIT License. See [LICENSE](LICENSE) for details.
