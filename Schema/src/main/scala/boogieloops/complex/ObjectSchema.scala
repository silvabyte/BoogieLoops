package boogieloops.schema.complex

import scala.collection.immutable.ListMap

import boogieloops.schema.Schema
import boogieloops.schema.validation.{ValidationResult, ValidationContext}

import scala.util.matching.Regex

/**
 * Object schema type with JSON Schema 2020-12 validation keywords
 */
case class ObjectSchema(
    properties: Map[String, Schema] = ListMap.empty,
    required: Set[String] = Set.empty,
    minProperties: Option[Int] = None,
    maxProperties: Option[Int] = None,
    additionalProperties: Option[Boolean] = None,
    additionalPropertiesSchema: Option[Schema] = None,
    patternProperties: Map[String, Schema] = Map.empty,
    propertyNames: Option[Schema] = None,
    dependentRequired: Map[String, Set[String]] = Map.empty,
    dependentSchemas: Map[String, Schema] = Map.empty,
    unevaluatedProperties: Option[Boolean] = None
) extends Schema {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("object"))

    // Properties
    if (properties.nonEmpty) {
      val propsMap = properties.map { case (key, value) =>
        key -> value.toJsonSchema
      }
      schema("properties") = ujson.Obj.from(propsMap)
    }

    // Required properties - ordered by properties key order when available
    if (required.nonEmpty) {
      val orderedRequired =
        properties.keys.filter(required.contains).toList ++ required.filterNot(properties.contains)
      schema("required") = ujson.Arr(orderedRequired.map(ujson.Str(_))*)
    }

    // Validation keywords
    minProperties.foreach(min => schema("minProperties") = ujson.Num(min))
    maxProperties.foreach(max => schema("maxProperties") = ujson.Num(max))

    // Additional properties - can be boolean or schema
    additionalProperties.foreach(add => schema("additionalProperties") = ujson.Bool(add))
    additionalPropertiesSchema.foreach(aps => schema("additionalProperties") = aps.toJsonSchema)

    // Pattern properties
    if (patternProperties.nonEmpty) {
      val patternPropsMap = patternProperties.map { case (pattern, schemaValue) =>
        pattern -> schemaValue.toJsonSchema
      }
      schema("patternProperties") = ujson.Obj.from(patternPropsMap)
    }

    // Property names validation
    propertyNames.foreach(pn => schema("propertyNames") = pn.toJsonSchema)

    // Dependent validation (2020-12 features)
    if (dependentRequired.nonEmpty) {
      val depReqMap = dependentRequired.map { case (prop, deps) =>
        prop -> ujson.Arr(deps.map(ujson.Str(_)).toSeq*)
      }
      schema("dependentRequired") = ujson.Obj.from(depReqMap)
    }

    if (dependentSchemas.nonEmpty) {
      val depSchemaMap = dependentSchemas.map { case (prop, schemaValue) =>
        prop -> schemaValue.toJsonSchema
      }
      schema("dependentSchemas") = ujson.Obj.from(depSchemaMap)
    }

    // Unevaluated properties (2020-12 feature)
    unevaluatedProperties.foreach(unevaluated =>
      schema("unevaluatedProperties") = ujson.Bool(unevaluated)
    )

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this object schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    value match {
      case obj: ujson.Obj =>
        // Inline validation logic
        val errors = validateObject(obj, context)
        if (errors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(errors)
        }
      case _ =>
        // Non-object ujson.Value type - return TypeMismatch error
        val error = boogieloops.schema.ValidationError.TypeMismatch("object", getValueType(value), context.path)
        ValidationResult.invalid(error)
    }
  }

  /**
   * Validate an object value against this schema with proper context tracking
   */
  private def validateObject(
      value: ujson.Obj,
      context: ValidationContext
  ): List[boogieloops.schema.ValidationError] = {

    // Min properties validation
    val minPropErrors = minProperties.fold(List.empty[boogieloops.schema.ValidationError]) { min =>
      if (value.value.size < min)
        List(boogieloops.schema.ValidationError.MinPropertiesViolation(min, value.value.size, context.path))
      else Nil
    }

    // Max properties validation
    val maxPropErrors = maxProperties.fold(List.empty[boogieloops.schema.ValidationError]) { max =>
      if (value.value.size > max)
        List(boogieloops.schema.ValidationError.MaxPropertiesViolation(max, value.value.size, context.path))
      else Nil
    }

    // Required properties validation
    val requiredErrors = required.flatMap { prop =>
      if (!value.value.contains(prop))
        List(boogieloops.schema.ValidationError.MissingField(prop, context.path))
      else Nil
    }

    // Property validation - validate each property against its schema
    val propertyErrors = properties.flatMap { case (propName, propSchema) =>
      value.value.get(propName).fold(List.empty[boogieloops.schema.ValidationError]) { propValue =>
        val propContext = context.withProperty(propName)
        val validationResult = propSchema.validate(propValue, propContext)
        if (!validationResult.isValid) validationResult.errors else Nil
      }
    }

    // Pattern properties validation
    val patternErrors = patternProperties.flatMap { case (pattern, patternSchema) =>
      val regex = new Regex(pattern)
      value.value.flatMap { case (propName, propValue) =>
        if (regex.findFirstIn(propName).isDefined && !properties.contains(propName)) {
          val propContext = context.withProperty(propName)
          val validationResult = patternSchema.validate(propValue, propContext)
          if (!validationResult.isValid) validationResult.errors else Nil
        } else Nil
      }
    }

    // Additional properties validation
    val definedProperties = properties.keySet
    val patternMatchedProperties = patternProperties.keys.flatMap { pattern =>
      val regex = new Regex(pattern)
      value.value.keys.filter(propName => regex.findFirstIn(propName).isDefined)
    }.toSet

    val additionalProps = value.value.keySet.diff(definedProperties).diff(patternMatchedProperties)

    val additionalPropErrors = additionalProperties match {
      case Some(false) =>
        // Additional properties not allowed
        additionalProps.map { prop =>
          boogieloops.schema.ValidationError.AdditionalProperty(prop, context.path)
        }.toList
      case _ =>
        // Additional properties allowed or unspecified
        additionalPropertiesSchema match {
          case Some(additionalSchema) =>
            // Validate additional properties against the additional properties schema
            additionalProps.flatMap { propName =>
              value.value.get(propName).fold(List.empty[boogieloops.schema.ValidationError]) { propValue =>
                val propContext = context.withProperty(propName)
                val validationResult = additionalSchema.validate(propValue, propContext)
                if (!validationResult.isValid) validationResult.errors else Nil
              }
            }.toList
          case None =>
            // No additional properties schema - allow any additional properties
            Nil
        }
    }

    (minPropErrors ++ maxPropErrors ++ requiredErrors ++ propertyErrors ++ patternErrors ++ additionalPropErrors).reverse
  }

  /**
   * Get string representation of ujson.Value type for error messages
   */
  private def getValueType(value: ujson.Value): String = {
    value match {
      case _: ujson.Str => "string"
      case _: ujson.Num => "number"
      case _: ujson.Bool => "boolean"
      case ujson.Null => "null"
      case _: ujson.Arr => "array"
      case _: ujson.Obj => "object"
    }
  }
}
