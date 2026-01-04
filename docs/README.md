# Getting Started

Welcome to the BoogieLoops ecosystem — Scala 3 libraries that work great together or stand alone:

- **schema**: Type-safe JSON Schema (derive, build, validate)
- **web**: Schema-first HTTP validation + OpenAPI for Cask
- **ai**: Type-safe LLM agents with structured output
- **kit**: General purpose utilities (DotEnv, etc.)

## Prerequisites

- Scala 3.6.2+
- JDK 17+
- Mill (launcher included as `./mill`)

## Install

Mill:

```scala
mvn"dev.boogieloop::schema:0.6.0"  // Core schemas
mvn"dev.boogieloop::web:0.6.0"     // HTTP validation + OpenAPI
mvn"dev.boogieloop::ai:0.6.0"      // LLM agents
mvn"dev.boogieloop::kit:0.6.0"     // General utilities
```

SBT:

```scala
libraryDependencies ++= Seq(
  "dev.boogieloop" %% "schema" % "0.6.0",
  "dev.boogieloop" %% "web" % "0.6.0",
  "dev.boogieloop" %% "ai" % "0.6.0",
  "dev.boogieloop" %% "kit" % "0.6.0"
)
```

## Common Commands

- Build: `make build` (or `./mill __.compile`)
- Test: `make test` (override `MODULE=schema|web|ai|kit`)
- Lint: `make lint` (scalafix)
- Format: `make format` (scalafmt)
- Watch: `make watch MODULE=schema` or `make watch-test MODULE=web`
- Examples: `make schema`, `make web`, `make ai`

Run `make help` for the full list.

## Examples Overview

- `make schema`: Run core schema examples
- `make web`: Start the web API example on port 8082
- `make web-upload`: Start the Upload/Streaming demo (defaults to port 8080)
- `make ai`: Run ai examples (requires API keys or a local LLM)

AI examples environment variables:

```bash
export OPENAI_API_KEY="your-openai-key"           # OpenAI provider
export ANTHROPIC_API_KEY="your-anthropic-key"     # Anthropic provider (optional)
```

Local LLMs via OpenAI-compatible endpoints (no key by default) are supported (LM Studio, Ollama, llama.cpp).

## Next Steps

- schema (core): [schema.md](./schema.md)
- web: [web.md](./web.md)
- ai: [ai.md](./ai.md)
- kit: [kit.md](./kit.md)

## More Docs

- Concepts: [concepts.md](./concepts.md)
- Zero to App: [zero-to-app.md](./zero-to-app.md)
- Troubleshooting: [troubleshooting.md](./troubleshooting.md)
- TypeScript SDK: [typescript-sdk.md](./typescript-sdk.md)
