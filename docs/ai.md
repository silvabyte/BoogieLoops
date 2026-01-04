# BoogieLoops AI: Type‑Safe LLM Agents

LLM agents with multi‑provider support (OpenAI, Anthropic, OpenAI‑compatible local endpoints), typed structured output using BoogieLoops Schema schemas, scoped history, and hooks/metrics.

## Install

Mill:

```scala
mvn"dev.boogieloop::schema:0.6.0"
mvn"dev.boogieloop::ai:0.6.0"
```

SBT:

```scala
libraryDependencies ++= Seq(
  "dev.boogieloop" %% "schema" % "0.6.0",
  "dev.boogieloop" %% "ai" % "0.6.0"
)
```

## Quickstart

```scala
import boogieloops.ai.agent.*
import boogieloops.ai.agent.providers.OpenAIProvider
import boogieloops.schema.derivation.Schema

@Schema.title("Summary")
case class Summary(@Schema.minLength(1) text: String) derives Schema

val agent = Agent(
  name = "Summarizer",
  instructions = "Summarize briefly.",
  provider = new OpenAIProvider(sys.env("OPENAI_API_KEY")),
  model = "gpt-4o-mini"
)

val md = RequestMetadata(userId = Some("dev"))
val res = agent.generateObject[Summary]("Summarize: Scala 3 match types.", md)
```

## Local LLMs (OpenAI‑Compatible)

```scala
import boogieloops.ai.agent.providers.OpenAICompatibleProvider

val local = OpenAICompatibleProvider(
  baseUrl = "http://localhost:1234/v1",
  modelId = "local-model",
  strictModelValidation = false
)

val agent = Agent("Local", "You are helpful.", local, model = "local-model")
```

## Structured Output

- Define your data with BoogieLoops Schema annotations; use `agent.generateObject[T]`.
- On success you get `Right(ObjectResponse[T])`; failures return `Left(SchemaError)`.

## Metadata & Scoping

- Provide `RequestMetadata(tenantId, userId, conversationId)` to scope history.
- Use `generateTextWithoutHistory` for stateless calls.

→ [Detailed Guide: Metadata & Scoping](./ai/metadata-and-scoping.md)

## Hooks & Metrics

- Register hooks for logging/metrics/tracing; combine multiple hooks.
- Built‑in metrics helpers available; export to Prometheus or JSON if desired.

→ [Detailed Guide: Hooks & Metrics](./ai/hooks-and-metrics.md)

## Run Examples

- `make ai` (requires `OPENAI_API_KEY` or a local LLM endpoint)
- Local LLM: ensure your server exposes `/v1` and a valid model ID.
