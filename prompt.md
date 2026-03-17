# Remove ~30 Field Derivation Limit via Macro Migration

## Context

`derives Schematic` fails to compile on case classes with >30 fields. Five inline recursive methods (`getElemLabels`, `getElemSchemas`, `getElemSchema`, `getRequiredFields`, `getRequiredFieldsWithDefaultsHelper`) each consume N inline expansions for N fields, exceeding Scala 3's default limit of 32. This is a fundamental limitation — real-world data models (API responses, DB records) regularly exceed 30 fields.

The `AnnotationProcessor` already demonstrates the fix: iterate fields via `primaryConstructor.paramSymss.flatten` in a macro, consuming zero inline budget. We will apply this same pattern to product type (case class) derivation.

**Branch**: `fix/preserve-field-order-json-schema` (continue from existing work on this branch)

---

## Task 1: Add `DerivedFieldInfo` data class

**File**: `Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala`

Add a case class at the package level (before the `Schematic` object) that carries all per-field information extracted by the macro:

```scala
case class DerivedFieldInfo(
  labels: List[String],
  schemas: List[Schema],
  isOption: List[Boolean],
  hasScalaDefault: List[Boolean]
)
```

This replaces the information that was previously computed by five separate inline recursive methods.

---

## Task 2: Implement the macro bridge and implementation

**File**: `Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala`

### 2a. Add the inline macro bridge inside the `Schematic` object:

```scala
inline def deriveProductFields[T]: DerivedFieldInfo =
  ${ deriveProductFieldsImpl[T] }
```

### 2b. Implement `deriveProductFieldsImpl[T: Type](using Quotes): Expr[DerivedFieldInfo]`

This macro:
1. Gets fields via `TypeRepr.of[T].typeSymbol.primaryConstructor.paramSymss.flatten`
2. For each field symbol, extracts:
   - **Name**: `fieldSym.name`
   - **Schema**: maps the field's type to a `Schema` expression (see helper below)
   - **isOption**: `true` if the field type is `Option[_]`
   - **hasScalaDefault**: checks companion object for `$lessinit$greater$default$N` method (reuse logic from existing `hasScalaDefaultValueImpl` at line 343)
3. Builds `Expr[DerivedFieldInfo]` from the collected data

**Reference**: Follow the same pattern as `AnnotationProcessor.extractAllFieldAnnotationsImpl[T]` at lines 378–401 of `AnnotationProcessor.scala`.

### 2c. Implement `schemaForType` private helper method

This replaces the inline `getElemSchema` method (lines 254–267). It maps a `TypeRepr` to an `Expr[Schema]`:

- `String` → `'{ bl.String() }`
- `Int` → `'{ bl.Integer() }`
- `Long` → `'{ bl.Integer() }`
- `Double` → `'{ bl.Number() }`
- `Float` → `'{ bl.Number() }`
- `Boolean` → `'{ bl.Boolean() }`
- `Option[T]` → get inner type via `tpe.typeArgs.head`, recurse, wrap with `.optional`
- `List[T]` → get inner type via `tpe.typeArgs.head`, recurse, wrap with `bl.Array(...)`
- Any other type → `Expr.summon[Schematic[t]]`, call `.schema` on it. If no instance found, use `report.errorAndAbort` with a clear message suggesting `derives Schematic` or providing a `given`.

**Important**: Use `tpe.dealias` before matching to handle type aliases correctly. Use `tpe.typeArgs.head` for extracting inner types from `Option` and `List`. Use `tpe.typeSymbol == TypeRepr.of[Option[?]].typeSymbol` for Option checking (similarly for List).

### 2d. Implement `isOptionType` and `isListType` private helpers

```scala
private def isOptionType(using Quotes)(tpe: quotes.reflect.TypeRepr): Boolean
private def isListType(using Quotes)(tpe: quotes.reflect.TypeRepr): Boolean
```

### 2e. Implement `hasDefaultParam` private helper

Reuse the existing logic from `hasScalaDefaultValueImpl` (lines 343–369):
- Get companion object symbol
- Check for method named `$lessinit$greater$default$N` (1-indexed)
- Wrap in try-catch, defaulting to `false` on any reflection failure

### 2f. Get the field type correctly

Use `fieldSym.termRef.widenTermRefByName` to get each field's type. If this doesn't work for edge cases, fall back to pattern matching on `fieldSym.tree` as a `ValDef` and extracting `tpt.tpe`.

---

## Task 3: Rewrite `deriveProductWithAnnotations`

**File**: `Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala` (lines 92–123)

Replace the current implementation that uses inline recursion with:

```scala
inline def deriveProductWithAnnotations[T](p: Mirror.ProductOf[T]): Schema = {
  // Single macro call extracts all field info — no inline recursion
  val info = deriveProductFields[T]

  // Existing macro call for annotations — already non-recursive
  val fieldAnnotations = AnnotationProcessor.extractAllFieldAnnotations[T]

  // Apply annotations to schemas (runtime, no inline)
  val elemSchemasWithAnnotations = info.labels.zip(info.schemas).map {
    case (label, schema) =>
      fieldAnnotations.get(label) match {
        case Some(metadata) if metadata.nonEmpty =>
          AnnotationProcessor.applyMetadata(schema, metadata)
        case _ => schema
      }
  }

  val properties = info.labels.zip(elemSchemasWithAnnotations).to(ListMap)

  // Compute required fields at runtime using pre-computed isOption and hasScalaDefault
  val required = info.labels.zipWithIndex.collect {
    case (label, idx) if !info.isOption(idx) =>
      val hasAnnotationDefault = fieldAnnotations.get(label).exists(_.default.isDefined)
      val hasDefault = hasAnnotationDefault || info.hasScalaDefault(idx)
      if (!hasDefault) Some(label) else None
  }.flatten.toSet

  val baseObjectSchema = bl.Object(
    properties = properties,
    required = required
  )

  // Apply class-level annotations
  val classMetadata = AnnotationProcessor.extractClassAnnotations[T]
  AnnotationProcessor.applyMetadata(baseObjectSchema, classMetadata)
}
```

The method stays `inline` (needed for `Mirror` dispatch in `derived`) but contains NO inline recursion — just two macro calls with O(1) inline depth.

---

## Task 4: Rewrite `deriveProduct`

**File**: `Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala` (lines 77–87)

```scala
inline def deriveProduct[T](p: Mirror.ProductOf[T]): Schema = {
  val info = deriveProductFields[T]
  val properties = info.labels.zip(info.schemas).to(ListMap)
  val required = info.labels.zipWithIndex.collect {
    case (label, idx) if !info.isOption(idx) => label
  }.toSet

  bl.Object(
    properties = properties,
    required = required
  )
}
```

---

## Task 5: Remove obsolete inline methods

**File**: `Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala`

Remove these methods that are no longer called (only were used by product derivation):

- `getRequiredFields[Elems <: Tuple]` (line 272)
- `getRequiredFieldsWithDefaults[T, Elems <: Tuple]` (line 289)
- `getRequiredFieldsWithDefaultsHelper[T, Elems <: Tuple]` (line 298)
- `hasScalaDefaultValue[T]` inline bridge (line 337)
- `hasScalaDefaultValueImpl[T]` macro impl (line 343)

**Keep** these methods (still used by sum type derivation — variants are few, limit not hit):
- `getElemLabels` — used by `deriveSum`/`deriveSumWithAnnotations`
- `getElemSchemas` / `getElemSchema` — used by sum type derivation
- `isSimpleEnum` / `isEmptyProductType` — sum type only

---

## Task 6: Run existing tests — all must pass

```bash
./mill Schema.test
```

All 129 existing tests must pass unchanged. Key test files to watch:
- `DefaultAnnotationTests.scala` — validates `@Schematic.default` + Scala default parameter detection still works after macro migration
- `SealedTraitDerivationTests.scala` — validates sum type derivation (uses kept inline methods, should be unaffected)
- `FieldOrderTests.scala` — validates ListMap field ordering
- `MapDerivationTests.scala`, `SetDerivationTests.scala`, `VectorDerivationTests.scala` — validates CollectionSchemas givens are still found via `Expr.summon`

If any test fails, debug and fix before proceeding. The public API (`derives Schematic`, `Schematic[T]`, annotations) must not change.

---

## Task 7: Add 50-field `derives Schematic` test

**File**: `Schema/test/src/boogieloops/derivation/FieldOrderTests.scala`

This is the proof that the field limit is lifted. Add a case class with 50 fields that uses `derives Schematic`:

```scala
// 50-field case class — previously impossible due to inline recursion limit
// format: off
case class FiftyFieldsDerived(
    f01: String, f02: Int, f03: Double, f04: Boolean, f05: String,
    f06: Int, f07: Double, f08: Boolean, f09: String, f10: Int,
    f11: Double, f12: Boolean, f13: String, f14: Int, f15: Double,
    f16: Boolean, f17: String, f18: Int, f19: Double, f20: Boolean,
    f21: String, f22: Int, f23: Double, f24: Boolean, f25: String,
    f26: Int, f27: Double, f28: Boolean, f29: String, f30: Int,
    f31: Double, f32: Boolean, f33: String, f34: Int, f35: Double,
    f36: Boolean, f37: String, f38: Int, f39: Double, f40: Boolean,
    f41: String, f42: Int, f43: Double, f44: Boolean, f45: String,
    f46: Int, f47: Double, f48: Boolean, f49: String, f50: Int
) derives Schematic
// format: on
```

Add these test cases:

```scala
test("50-field derived case class compiles and preserves field order") {
  val schema = Schematic[FiftyFieldsDerived]
  val json = schema.toJsonSchema
  val keys = json("properties").obj.keys.toList
  val expectedKeys = (1 to 50).map(i => f"f$i%02d").toList
  assert(keys == expectedKeys)
}

test("50-field derived case class has correct required fields") {
  val schema = Schematic[FiftyFieldsDerived]
  val json = schema.toJsonSchema
  val required = json("required").arr.map(_.str).toList
  val expectedKeys = (1 to 50).map(i => f"f$i%02d").toList
  assert(required == expectedKeys)
}
```

Keep the existing manual `bl.Object` 50-field test as well — it tests a different code path.

Update the comment on the manual test from:
```
// (derived case classes hit the compiler's inline recursion limit past ~30 fields)
```
to:
```
// Tests manual bl.Object construction separately from derived case classes
```

---

## Task 8: Add edge case tests

**File**: `Schema/test/src/boogieloops/derivation/FieldOrderTests.scala`

Add tests that exercise the macro with complex field types at scale:

```scala
// 35-field case class with mixed types including Option, List, and nested types
case class MixedLargeClass(
    f01: String, f02: Option[String], f03: Int, f04: Option[Int], f05: Double,
    f06: List[String], f07: Boolean, f08: Option[Boolean], f09: String, f10: Int,
    f11: Double, f12: List[Int], f13: String, f14: Option[Double], f15: Int,
    f16: Boolean, f17: String, f18: Int, f19: Double, f20: Option[String],
    f21: String, f22: Int, f23: List[Double], f24: Boolean, f25: String,
    f26: Int, f27: Double, f28: Boolean, f29: String, f30: Int,
    f31: Option[List[String]], f32: Boolean, f33: String, f34: Int, f35: Double
) derives Schematic

test("35-field class with mixed types preserves order and required") {
  val schema = Schematic[MixedLargeClass]
  val json = schema.toJsonSchema
  val keys = json("properties").obj.keys.toList
  val expectedKeys = (1 to 35).map(i => f"f$i%02d").toList
  assert(keys == expectedKeys)

  val required = json("required").arr.map(_.str).toSet
  // Option fields should NOT be in required
  assert(!required.contains("f02"))
  assert(!required.contains("f04"))
  assert(!required.contains("f08"))
  assert(!required.contains("f14"))
  assert(!required.contains("f20"))
  assert(!required.contains("f31"))
  // Non-option fields should be in required
  assert(required.contains("f01"))
  assert(required.contains("f03"))
  assert(required.contains("f06"))
}
```

---

## Task 9: Run full test suite — final verification

```bash
./mill Schema.test
```

All tests must pass. Verify the test count has increased from the previous 129.

---

## Task 10: Commit and push

Stage and commit all changes:

```bash
git add Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala \
       Schema/test/src/boogieloops/derivation/FieldOrderTests.scala
git commit -m "fix(schema): remove ~30 field derivation limit via macro migration

Replace five inline recursive methods (getElemLabels, getElemSchemas,
getRequiredFields, etc.) with a single macro that iterates fields via
primaryConstructor.paramSymss.flatten, consuming zero inline budget.

Product derivation now works for case classes with any number of fields.
Sum type derivation retains inline methods (variants are always few).

Add 50-field and 35-field derives Schematic tests proving the limit
is lifted.

Resolves the inline recursion depth compilation failure for large
case classes."
git push
```

---

## Key Reference Files

- `Schema/src/main/scala/boogieloops/derivation/AnnotationProcessor.scala:378–401` — model for macro field iteration pattern (follow this pattern)
- `Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala:254–267` — `getElemSchema` type→schema mapping logic to port into macro
- `Schema/src/main/scala/boogieloops/derivation/SchemaDerivation.scala:343–369` — `hasScalaDefaultValueImpl` default detection logic to reuse
- `Schema/src/main/scala/boogieloops/derivation/CollectionSchemas.scala` — `Map`/`Set`/`Vector` givens that must remain summonable via `Expr.summon`
- `Schema/src/main/scala/boogieloops/Schema.scala` — `bl.Object` factory (no changes needed)

## Files NOT to modify

- `AnnotationProcessor.scala` — already uses macros, works fine
- `Schema.scala` — core trait, unchanged
- `ObjectSchema.scala` — runtime schema class, unchanged
- `CollectionSchemas.scala` — collection givens, unchanged
