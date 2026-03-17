package boogieloops.schema.derivation

import scala.collection.immutable.ListMap

import utest.*
import boogieloops.schema.*
import boogieloops.schema.complex.*

object FieldOrderTests extends TestSuite {

  // Case class with 6 fields — enough to exceed the 4-element threshold
  // where Scala's immutable.Map stops preserving insertion order
  case class SixFields(a: String, b: Int, c: Double, d: Boolean, e: String, f: Int) derives Schematic

  // Case class with 6 annotated fields
  case class AnnotatedSixFields(
      @Schematic.description("field a") a: String,
      @Schematic.description("field b") b: Int,
      @Schematic.description("field c") c: Double,
      @Schematic.description("field d") d: Boolean,
      @Schematic.description("field e") e: String,
      @Schematic.description("field f") f: Int
  ) derives Schematic

  // Sealed trait with a variant that has 5+ fields
  sealed trait Discriminated derives Schematic
  case class BigVariant(a: String, b: Int, c: Double, d: Boolean, e: String)
      extends Discriminated derives Schematic

  val tests = Tests {
    test("Derived schema preserves field order with 5+ fields") {
      val schema = Schematic[SixFields]
      val json = schema.toJsonSchema
      val keys = json("properties").obj.keys.toList

      assert(keys == List("a", "b", "c", "d", "e", "f"))
    }

    test("Required array preserves field order") {
      val schema = Schematic[SixFields]
      val json = schema.toJsonSchema
      val required = json("required").arr.map(_.str).toList

      assert(required == List("a", "b", "c", "d", "e", "f"))
    }

    test("Derived schema with annotations preserves field order") {
      val schema = Schematic[AnnotatedSixFields]
      val json = schema.toJsonSchema
      val keys = json("properties").obj.keys.toList

      assert(keys == List("a", "b", "c", "d", "e", "f"))
    }

    test("Manual bl.Object varargs preserves order") {
      val schema = bl.Object(
        "z" -> bl.String(),
        "a" -> bl.Integer(),
        "m" -> bl.Boolean()
      )
      val json = schema.toJsonSchema
      val keys = json("properties").obj.keys.toList

      assert(keys == List("z", "a", "m"))
    }

    test("Sealed trait discriminator preserves field order") {
      val schema = Schematic[Discriminated]
      val json = schema.toJsonSchema
      val variant = json("oneOf").arr.head
      val keys = variant("properties").obj.keys.toList

      // "type" prepended, then fields in declaration order
      assert(keys == List("type", "a", "b", "c", "d", "e"))

      // required should also reflect this order
      val required = variant("required").arr.map(_.str).toList
      assert(required == List("type", "a", "b", "c", "d", "e"))
    }
  }
}
