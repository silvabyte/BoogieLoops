package boogieloops.schema.derivation

import scala.collection.immutable.ListMap
import scala.deriving.*
import scala.compiletime.*
import scala.quoted.*
import boogieloops.schema.*
import boogieloops.schema.bl

import boogieloops.schema.validation.ValidationContext
import upickle.default.*

/**
 * Type class for automatically deriving JSON Schema from case class definitions
 *
 * Usage: case class User(id: String, name: String) derives Schematic
 */
trait Schematic[T] {
  def schema: Schema
}

/**
 * Data class that carries all per-field information extracted by the macro.
 * This replaces the information that was previously computed by five separate
 * inline recursive methods, avoiding the inline recursion limit for large case classes.
 */
case class DerivedFieldInfo(
    labels: List[String],
    schemas: List[Schema],
    isOption: List[Boolean],
    hasScalaDefault: List[Boolean]
)

object Schematic {

  /**
   * Get the schema for a type T
   */
  def apply[T](using s: Schematic[T]): Schema = s.schema

  /**
   * Create a Schematic instance from a Schema
   */
  def instance[T](s: Schema): Schematic[T] = new Schematic[T]:
    def schema: Schema = s

  // ========================================
  // Annotation Type Aliases to be able to do @Schematic.title("...") etc.
  // ========================================

  type title = SchemaAnnotations.title
  type description = SchemaAnnotations.description
  type format = SchemaAnnotations.format
  type minLength = SchemaAnnotations.minLength
  type maxLength = SchemaAnnotations.maxLength
  type minimum = SchemaAnnotations.minimum
  type maximum = SchemaAnnotations.maximum
  type pattern = SchemaAnnotations.pattern
  type minItems = SchemaAnnotations.minItems
  type maxItems = SchemaAnnotations.maxItems
  type uniqueItems = SchemaAnnotations.uniqueItems
  type multipleOf = SchemaAnnotations.multipleOf
  type exclusiveMinimum = SchemaAnnotations.exclusiveMinimum
  type exclusiveMaximum = SchemaAnnotations.exclusiveMaximum
  type enumValues = SchemaAnnotations.enumValues
  type const = SchemaAnnotations.const
  type default = SchemaAnnotations.default
  type examples = SchemaAnnotations.examples
  type readOnly = SchemaAnnotations.readOnly
  type writeOnly = SchemaAnnotations.writeOnly
  type deprecated = SchemaAnnotations.deprecated

  /**
   * Derive a Schematic instance using Mirror-based reflection with annotation processing
   *
   * This is the enhanced core derivation method that analyzes case class structure, processes annotations, and generates the
   * corresponding JSON Schema automatically with rich metadata.
   */
  inline def derived[T](using m: Mirror.Of[T]): Schematic[T] = {
    val derivedSchema = inline m match
      case p: Mirror.ProductOf[T] => deriveProductWithAnnotations[T](p)
      case s: Mirror.SumOf[T] => deriveSumWithAnnotations[T](s)
    instance[T](derivedSchema)
  }

  /**
   * Derive schema for product types (case classes) - original method
   */
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

  /**
   * Derive schema for product types (case classes) with annotation processing
   */
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

  /**
   * Derive schema for sum types (enums/sealed traits)
   */
  inline def deriveSum[T](s: Mirror.SumOf[T]): Schema = {
    val elemLabels = getElemLabels[s.MirroredElemLabels]

    // For Scala 3 enums, we detect them by checking if all element types are singletons
    // If so, generate a string enum schema instead of trying to derive schemas for each case
    inline if (isSimpleEnum[s.MirroredElemTypes]) {
      // Generate enum schema for simple Scala 3 enums using EnumSchema
      bl.StringEnum(elemLabels)
    } else {
      // For complex sum types, derive schemas and use OneOf
      val elemSchemas = getElemSchemas[T, s.MirroredElemTypes]
      if elemSchemas.nonEmpty then
        bl.OneOf(elemSchemas*)
      else
        bl.String()
    }
  }

  /**
   * Check if this is a simple enum by examining the element types at compile time
   * Simple enums have singleton cases with empty parameter lists
   * Sealed traits have case classes with non-empty parameter lists
   */
  inline def isSimpleEnum[Elems <: Tuple]: Boolean = {
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        // Use a different approach: check if this type can be treated as a simple singleton
        // by trying to examine its structure more directly
        inline if (isEmptyProductType[head]) {
          isSimpleEnum[tail] // Continue checking if this is an empty product (enum case)
        } else {
          false // Has fields, so not a simple enum
        }
      case _: EmptyTuple => true
  }

  /**
   * Check if a type is an empty product type (no constructor parameters)
   * This distinguishes enum cases from case classes with parameters
   */
  inline def isEmptyProductType[T]: Boolean = {
    summonFrom {
      case mirror: Mirror.ProductOf[T] =>
        // Check if the mirrored element types is EmptyTuple
        inline erasedValue[mirror.MirroredElemTypes] match
          case _: EmptyTuple => true // No parameters = enum case
          case _ => false // Has parameters = case class
      case _ =>
        // If no Mirror.ProductOf, treat as enum case (true singleton)
        true
    }
  }

  /**
   * Derive schema for sum types (enums/sealed traits) with annotation processing
   */
  inline def deriveSumWithAnnotations[T](s: Mirror.SumOf[T]): Schema = {
    val elemLabels = getElemLabels[s.MirroredElemLabels]

    // For Scala 3 enums, we detect them by checking if all element types are singletons
    // If so, generate a string enum schema instead of trying to derive schemas for each case
    inline if (isSimpleEnum[s.MirroredElemTypes]) {
      // Generate enum schema for simple Scala 3 enums using EnumSchema
      bl.StringEnum(elemLabels)
    } else {
      // For sealed traits, derive schemas with discriminator field injection
      val elemSchemas = getElemSchemas[T, s.MirroredElemTypes]
      if elemSchemas.nonEmpty then
        // Add type discriminator field to each schema variant
        val discriminatedSchemas = elemLabels.zip(elemSchemas).map { case (typeName, schema) =>
          addTypeDiscriminator(schema, typeName)
        }
        bl.OneOf(discriminatedSchemas*)
      else {
        bl.String()
      }
    }
  }

  /**
   * Add a type discriminator field to a schema for sealed trait variants
   */
  def addTypeDiscriminator(schema: Schema, typeName: String): Schema = {
    schema match {
      case obj: boogieloops.schema.complex.ObjectSchema =>
        // Add "type" field with the variant name as a constant
        val typeField = bl.String(const = Some(typeName))
        val updatedProperties = ListMap("type" -> typeField) ++ obj.properties
        val updatedRequired = obj.required + "type"

        obj.copy(
          properties = updatedProperties,
          required = updatedRequired
        )
      case _ =>
        // For non-object schemas, wrap in an object with just the type field
        bl.Object(
          properties = ListMap("type" -> bl.String(const = Some(typeName))),
          required = Set("type")
        )
    }
  }

  /**
   * Get element labels as List[String]
   */
  inline def getElemLabels[Labels <: Tuple]: List[String] = {
    inline erasedValue[Labels] match
      case _: (head *: tail) =>
        constValue[head].asInstanceOf[String] :: getElemLabels[tail]
      case _: EmptyTuple => Nil
  }

  /**
   * Get element schemas by recursively deriving or summoning Schema instances
   */
  inline def getElemSchemas[T, Elems <: Tuple]: List[Schema] = {
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        getElemSchema[T, head] :: getElemSchemas[T, tail]
      case _: EmptyTuple => Nil
  }

  /**
   * Get schema for a single element type
   */
  inline def getElemSchema[T, Elem]: Schema = {
    inline erasedValue[Elem] match
      case _: String => bl.String()
      case _: Int => bl.Integer()
      case _: Long => bl.Integer()
      case _: Double => bl.Number()
      case _: Float => bl.Number()
      case _: Boolean => bl.Boolean()
      case _: Option[t] => getElemSchema[T, t].optional
      case _: List[t] => bl.Array(getElemSchema[T, t])
      case _ =>
        // For complex types, try to summon a Schematic instance
        summonInline[Schematic[Elem]].schema
  }

  /**
   * Basic Schematic instances for primitive types
   */
  given Schematic[String] = instance(bl.String())
  given Schematic[Int] = instance(bl.Integer())
  given Schematic[Long] = instance(bl.Integer())
  given Schematic[Double] = instance(bl.Number())
  given Schematic[Float] = instance(bl.Number())
  given Schematic[Boolean] = instance(bl.Boolean())

  given [T](using s: Schematic[T]): Schematic[Option[T]] = instance(s.schema.optional)
  given [T](using s: Schematic[T]): Schematic[List[T]] = instance(bl.Array(s.schema))

  /**
   * Macro bridge to extract all field information for product type derivation.
   * This consumes zero inline budget compared to recursive inline methods.
   */
  inline def deriveProductFields[T]: DerivedFieldInfo =
    ${ deriveProductFieldsImpl[T] }

  /**
   * Macro implementation that extracts all field information for product types.
   * Iterates fields via primaryConstructor.paramSymss.flatten to avoid inline recursion limit.
   */
  private def deriveProductFieldsImpl[T: Type](using Quotes): Expr[DerivedFieldInfo] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val typeSymbol = tpe.typeSymbol

    val fields = typeSymbol.primaryConstructor.paramSymss.flatten

    val fieldLabels = fields.map(_.name)

    val isOptionFlags = fields.map { fieldSym =>
      val fieldType = getFieldType(fieldSym)
      isOptionType(fieldType)
    }

    val hasDefaultFlags = fields.zipWithIndex.map { case (_, idx) =>
      hasDefaultParam(tpe, idx)
    }

    val schemaExprs = fields.map { fieldSym =>
      val fieldType = getFieldType(fieldSym)
      schemaForType(fieldType)
    }

    val labelsExpr = Expr.ofList(fieldLabels.map(Expr(_)))
    val schemasExpr = Expr.ofList(schemaExprs)
    val isOptionExpr = Expr.ofList(isOptionFlags.map(Expr(_)))
    val hasScalaDefaultExpr = Expr.ofList(hasDefaultFlags.map(Expr(_)))

    '{
      DerivedFieldInfo(
        labels = $labelsExpr,
        schemas = $schemasExpr,
        isOption = $isOptionExpr,
        hasScalaDefault = $hasScalaDefaultExpr
      )
    }
  }

  /**
   * Get the TypeRepr for a field symbol, handling edge cases.
   */
  private def getFieldType(using Quotes)(fieldSym: quotes.reflect.Symbol): quotes.reflect.TypeRepr = {
    import quotes.reflect.*
    fieldSym.termRef.widenTermRefByName
  }

  /**
   * Check if a type is Option[_].
   */
  private def isOptionType(using Quotes)(tpe: quotes.reflect.TypeRepr): Boolean = {
    import quotes.reflect.*
    val dealiased = tpe.dealias
    dealiased.typeSymbol == TypeRepr.of[Option[?]].typeSymbol
  }

  /**
   * Check if a type is List[_].
   */
  private def isListType(using Quotes)(tpe: quotes.reflect.TypeRepr): Boolean = {
    import quotes.reflect.*
    val dealiased = tpe.dealias
    dealiased.typeSymbol == TypeRepr.of[List[?]].typeSymbol
  }

  /**
   * Map a TypeRepr to an Expr[Schema].
   * This replaces the inline getElemSchema method.
   */
  private def schemaForType(using Quotes)(tpe: quotes.reflect.TypeRepr): Expr[Schema] = {
    import quotes.reflect.*

    val dealiased = tpe.dealias

    def isPrimitiveType: Option[Expr[Schema]] = {
      // Only match concrete primitive types, not type parameters
      if (dealiased.typeSymbol.isTypeParam) None
      else if (dealiased =:= TypeRepr.of[String]) Some('{ bl.String() })
      else if (dealiased =:= TypeRepr.of[Int]) Some('{ bl.Integer() })
      else if (dealiased =:= TypeRepr.of[Long]) Some('{ bl.Integer() })
      else if (dealiased =:= TypeRepr.of[Double]) Some('{ bl.Number() })
      else if (dealiased =:= TypeRepr.of[Float]) Some('{ bl.Number() })
      else if (dealiased =:= TypeRepr.of[Boolean]) Some('{ bl.Boolean() })
      else None
    }

    def handleOptionType: Option[Expr[Schema]] = {
      if (isOptionType(dealiased)) {
        val innerType = dealiased.typeArgs.head
        val innerSchema = schemaForType(innerType)
        Some('{ $innerSchema.optional })
      } else None
    }

    def handleListType: Option[Expr[Schema]] = {
      if (isListType(dealiased)) {
        val innerType = dealiased.typeArgs.head
        val innerSchema = schemaForType(innerType)
        Some('{ bl.Array($innerSchema) })
      } else None
    }

    def summonSchematic: Expr[Schema] = {
      import quotes.reflect.*
      // For type parameters or complex types, use Implicits.search which is safer than .asType
      val schematicType = TypeRepr.of[Schematic].appliedTo(dealiased)
      Implicits.search(schematicType) match {
        case result: ImplicitSearchSuccess =>
          // Build an expression that calls .schema on the found Schematic instance
          // Use Select to access the .schema field/method directly
          val schematicTree = result.tree
          val schemaSelect = Select.unique(schematicTree, "schema")
          schemaSelect.asExprOf[Schema]
        case failure: ImplicitSearchFailure =>
          report.errorAndAbort(
            s"Cannot derive Schematic for type ${tpe.show}. ${failure.explanation} " +
              "Please ensure the type either: (1) is a primitive type (String, Int, Boolean, etc.), " +
              "(2) is Option[T] or List[T] where T has a Schematic, or " +
              "(3) has a given Schematic instance in scope or uses `derives Schematic`."
          )
      }
    }

    isPrimitiveType
      .orElse(handleOptionType)
      .orElse(handleListType)
      .getOrElse(summonSchematic)
  }

  /**
   * Check if a field at the given index has a default value in the case class definition.
   * Uses compile-time reflection to detect Scala default parameters.
   */
  private def hasDefaultParam(using Quotes)(tpe: quotes.reflect.TypeRepr, fieldIndex: Int): Boolean = {
    import quotes.reflect.*
    try {
      val companionSym = tpe.typeSymbol.companionModule
      if (companionSym == Symbol.noSymbol) {
        false
      } else {
        val defaultMethodName = s"$$lessinit$$greater$$default$$${fieldIndex + 1}"
        val companionType = companionSym.termRef
        companionType.typeSymbol.declaredMethod(defaultMethodName).nonEmpty
      }
    } catch {
      case _ => false
    }
  }

}

/**
 * Enhanced ReadWriter derivation that combines Schematic with upickle ReadWriter
 */
object ValidatedReadWriter {

  /**
   * Create a ReadWriter that validates against the derived schema
   */
  def derived[T](using s: Schematic[T], rw: ReadWriter[T]): ReadWriter[T] = {
    readwriter[ujson.Value].bimap[T](
      // Writer: T -> ujson.Value (use case class ReadWriter)
      value => writeJs(value),
      // Reader: ujson.Value -> T (validate against schema, then deserialize)
      json => {
        // Validate against the derived schema
        val validationResult = s.schema.validate(json, ValidationContext())

        if (validationResult.isValid) {
          // Validation passed, deserialize with the case class ReadWriter
          read[T](json)
        } else {
          // Validation failed, create a meaningful error message
          val errorMessages = validationResult.errors.map(_.toString).mkString(", ")
          // scalafix:off DisableSyntax.throw
          // Disabling because throwing an exception is the appropriate way to handle schema validation
          // failures in the default validator implementation - users can override with custom validators
          throw new IllegalArgumentException(s"Schema validation failed: $errorMessages")
          // scalafix:on DisableSyntax.throw
        }
      }
    )
  }
}
