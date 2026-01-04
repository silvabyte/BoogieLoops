# Changelog

## [0.6.0] - 2026-01-04

* feat: add MagicNumber file type detection utility (#25)
* docs: sync installation versions to 0.5.6

## [0.5.6] - 2026-01-01

* feat: Add Kit module with DotEnv utility (#24)
* docs: sync installation versions to 0.5.5

## [0.5.5] - 2025-12-23

* fix: use boolean for strict in json_schema response_format (#23)
* docs: sync installation versions to 0.5.4

## [0.5.4] - 2025-12-22

* Fix OpenAPI jsonSchemaDialect warning (#22)
* chore: add ai disclaimer
* docs: sync installation versions to 0.5.3

## [0.5.3] - 2025-12-19

* fix: OpenAPI models serialize as flat maps per spec (#21)
* bd sync: 2025-12-18 18:18:37
* docs: sync installation versions to 0.5.2

## [0.5.2] - 2025-12-18

* fix: return raw string values from ValidatedRequest accessors (#20)
* docs: sync installation versions to 0.5.1
* docs: sync installation versions to 0.5.0 and add auto-sync to release workflow

## [0.5.1] - 2025-12-17

* chore: rename organization from dev.boogieloops to dev.boogieloop
* fix: remove redundant imports and suppress unused parameter warning

## [0.5.1] - 2025-12-17

- chore: update GitHub URLs from Chez to BoogieLoops
- chore: remove le chez
- fix: correct ObjectSchemaTests assertions to match actual validation behavior
- refactor: rename Schema type class to Schematic and fix directory casing
- chore: refactor continued...
- feat: complete BoogieLoops rebrand
- chore: sync beads issue tracking
- docs: rename and rebrand to BoogieLoops ecosystem
- refactor: rename CaskChez to Web in web module
- chore: close Chez→Schema rename tracking bead
- chore: sync beads after schema rename migration
- feat: comprehensive Chez → Schema class rename migration
- feat: add beads for memory layer

## [0.4.0] - 2025-12-10

### BREAKING CHANGES - Project Rebrand

This release renames the entire project from **Chez** to **BoogieLoops** with no backwards compatibility.

#### Module Renames

- `Chez` → `schema` (core JSON Schema library)
- `CaskChez` → `web` (HTTP API framework)
- `ChezWiz` → `ai` (LLM agent framework)

#### Package Renames

- `chez.*` → `boogieloops.schema.*`
- `caskchez.*` → `boogieloops.web.*`
- `chezwiz.*` → `boogieloops.ai.*`

#### Class Renames

- `trait Chez` → `trait Schema`
- `object Chez` → `object bl` (factory methods: `bl.String()`, `bl.Object()`, etc.)
- `*Chez` classes → `*Schema` classes (e.g., `StringChez` → `StringSchema`)
- `ChezType` → `SchemaType`
- `object CaskChez` → `object Web`

#### Maven Coordinates

- Group ID: `com.silvabyte` → `dev.boogieloop`
- Artifacts: `chez`, `caskchez`, `chezwiz` → `schema`, `web`, `ai`

#### Migration Example

```scala
// Before (0.3.x)
import chez.*
import chez.derivation.Schema
val schema = Chez.String()

// After (0.4.0)
import boogieloops.schema.*
import boogieloops.schema.derivation.Schema
val schema = bl.String()
```

## [0.3.2] - 2025-09-14

- feat: pass-through decorators; multipart uploads and streaming (#19)

## [0.3.1] - 2025-09-09

- refactor: remove LLMProvider abstraction (#18)
- docs: update version

## [0.3.0] - 2025-09-05

- refactor: remove MetricsFactory (#17)
- docs: add detailed documentation for Chez and ChezWiz (#16)
- chore: remove z-a section (#15)
- docs: consolidate Getting Started in docs/README.md (#14)
- docs: overhaul docs, add Zero-to-App flow, and scaffold runnable example (#13)
- docs: improve installation instructions and modernize code examples (#12)

## [0.2.1] - 2025-08-27

- feat: introduce Le Chez brand identity and clean up imports (#11)
- fix: trigger Maven publish on release created event

## [0.2.0] - 2025-08-27

- fix: remove artifacts collections from release
- feat: add Maven Central publishing support (#10)
- Remove AgentFactory and improve documentation with progressive examples (#9)

## [0.1.8] - 2025-08-26

- refactor: improve code quality with scalafix compliance and warning suppression (#8)

## [0.1.7] - 2025-08-21

- Consolidate OpenAI-compatible providers into unified architecture (#7)

## [0.1.6] - 2025-08-08

- chore: update release order
- feat: Add Vector Embeddings Support to ChezWiz (#6)
- chore: release v0.1.5

## [0.1.5] - 2025-08-05

- feat: support messages and multimodal (#5)
- chore: release v0.1.4

## [0.1.4] - 2025-08-01

- feat: Add LM Studio and Custom Endpoint Provider Support (#4)
- chore: release v0.1.3

## [0.1.3] - 2025-07-25

- feat: Add hooks and metrics system to ChezWiz (#3)
- chore: release v0.1.2

## [0.1.2] - 2025-07-24

- feat: add agent request metadata (#2)
- chore: release v0.1.1

## [0.1.1] - 2025-07-22

- feat: add either and release automation (#1)
- refactor: format all the things
- feat: implement chez lib into chezwiz
- chore: update scala fmt max col
- chore: fix readme example
- feat:
- feat: add ChezWiz lib
- chore: remove missing-validations project
- feat: implement ValidatedReadWriter validtion
- chore: update readme make commands
- chore: update readme installation with todo
- chore: remove phony command, pun intended
- feat: add test server unit test
- chore: add codeloops projects note
- chore: move documents
- feat: add fromChezError impl
- chore: remove t7 note
- feat: complete T7 AllOf, Not, and IfThenElse composition validation
- chore: update claude md
- feat: add ArrayChez validation
- chore: mark t4 as completed
- chore: remove legacy handlers
- feat: update validation tests & format all
- feat: add ujson validations to primitives
- feat: implement core validation infrastructure for Chez library
- chore: remove more notes
- chore: remove missing types project
- chore: remove old make commands
- feat: add $defs support in Chez schema
- feat: add makefile commands
- Complete T14: Scala Case Class Default Parameter Detection
- feat: enhance sealed trait schema derivation
- feat: implement EnumChez
- feat: add simple enum dervication types
- feat: add schema derivation for Vector[T] with corresponding tests
- feat: add support for additionalPropertiesSchema in ObjectChez and derive collection schemas
- feat: add get required fields with defaults
- fix: restore default value handling in deriveProductWithAnnotations
- feat: update docs and examples
- feat: support default values in annotations and add tests
- feat: enhance validation and schema gen with tests
- chore: clean up
- refactor: update CaskChez object references
- chore: clean up Any approach
- feat: implement ArrayReadWriter for ArrayChez schemas
- fix: schema generation
- feat: add initial derived types and openapi gen
- chore: update readme
- docs: enhance README
- refactor: streamline route registration in SchemaEndpoint
- refactor: clean up RouteSchema and validation logic, add custom endpoints
- chore: add cask chez module
- chore: update readme
- chore: update readme
- refactor: Schemaz dir to Chez
- feat: add initial chez
- Initial commit
