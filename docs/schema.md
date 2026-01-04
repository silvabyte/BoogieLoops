# BoogieLoops Schema (Core): Type‑Safe JSON Schema for Scala 3

Define schemas next to your types with annotations, derive JSON Schema automatically, and validate JSON at runtime.

## Install

Mill:

```scala
mvn"dev.boogieloop::schema:0.6.0"
```

SBT:

```scala
libraryDependencies += "dev.boogieloop" %% "schema" % "0.6.0"
```

## Quickstart

```scala
import boogieloops.schema.derivation.Schema
import ujson.*

@Schema.title("User")
case class User(
  @Schema.minLength(1) name: String,
  @Schema.format("email") email: String,
  @Schema.minimum(0) age: Int
) derives Schema

val schema = Schema[User]
val json = Obj("name"->"Ada","email"->"ada@lovelace.org","age"->28)
schema.validate(json) // Right(()) or Left(List(...))
```

## Annotations

- Strings: `minLength`, `maxLength`, `pattern`, `format`, `const`
- Numbers: `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `multipleOf`, `const`
- Arrays: `minItems`, `maxItems`, `uniqueItems`
- Enums: `enumValues("a", 1, true, 3.14, null)`
- Metadata: `title`, `description`, `examples`, `deprecated`, `readOnly`, `writeOnly`
- Defaults: `default(value)` (type‑safe)

→ [Annotations: Full Docs](./schema/annotations.md)

## Unions, Option, Nulls

- `A | B` → JSON Schema `oneOf`
- `Option[T]` → optional field (may be absent)
- Nullable: use `.nullable` (e.g., `Schema.String().nullable`)

## Tips

- Validate at boundaries and use errors to drive responses.
- Pair with upickle for read/write when you need typed data.
