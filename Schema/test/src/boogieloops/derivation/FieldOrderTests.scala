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

  // 50 properties built manually to stress-test ListMap ordering at scale
  // Tests manual bl.Object construction separately from derived case classes
  private val fiftyFieldPairs: Seq[(String, Schema)] =
    (1 to 50).map(i => f"f$i%02d" -> bl.String())

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

    test("50-field schema preserves insertion order") {
      val schema = bl.Object(fiftyFieldPairs*)
      val json = schema.toJsonSchema
      val expectedKeys = (1 to 50).map(i => f"f$i%02d").toList

      val keys = json("properties").obj.keys.toList
      assert(keys == expectedKeys)
    }

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
  }
}
