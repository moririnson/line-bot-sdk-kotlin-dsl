package com.linecorp.bot.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.linecorp.bot.model.message.flex.component.Video
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import jakarta.annotation.Nullable
import java.io.OutputStreamWriter
import java.lang.reflect.Type

class ModelDslProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val packageNames = listOf(
            "com.linecorp.bot.model.message.flex.component",
            "com.linecorp.bot.model.message.flex.container",
            "com.linecorp.bot.model.message.flex.unit",
        )
        val symbols = packageNames.flatMap { resolver.getDeclarationsFromPackage(it).toList() }
        symbols.forEach {
            generateDslBuilderFromClass(
                className = it.simpleName.asString(),
                packageName = it.packageName.asString(),
            )
        }
        return emptyList()
    }

    private fun generateDslBuilderFromClass(className: String, packageName: String) {
        val clazz = try {
            Class.forName("$packageName.$className")
        } catch (e: Exception) {
            return
        }
        if (clazz.isInterface || clazz.methods.none { it.name == "toBuilder" }) {
            return
        }
        val fields = extractFields(clazz)
        val properties = fields.map { generatePropertySpec(it) }

        val classType = ClassName(packageName, className)
        val dslClassName = "${className}GeneratedDsl"
        val dslClass = TypeSpec.classBuilder(dslClassName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .also { b ->
                        properties.filter { !it.type.isNullable }.forEach { prop ->
                            b.addParameter(prop.name, prop.type)
                        }
                    }
                    .build())
            .addProperties(properties)
            .addFunction(FunSpec.builder("build")
                .returns(classType)
                .addCode("return %T.builder()\n", classType)
                .apply {
                    properties.forEach { prop ->
                        addCode("    .${prop.name}(${prop.name})\n")
                    }
                }
                .addCode("    .build()")
                .build())
            .build()

        val spec = FileSpec.builder(packageName, dslClassName)
            .addType(dslClass)
            .addFunction(
                FunSpec.builder(className)
                    .addParameter(
                        "init",
                        LambdaTypeName.get(receiver = ClassName(packageName, dslClassName), returnType = UNIT)
                    )
                    .returns(classType)
                    .addCode(
                        """
                |val dsl = $dslClassName()
                |dsl.init()
                |return dsl.build()
                """.trimMargin()
                    )
                    .build()
            )
            .build()

        val file = try {
            codeGenerator.createNewFile(
                Dependencies.ALL_FILES,
                packageName,
                dslClassName
            )
        } catch (e: FileAlreadyExistsException) {
            // ???????? why ?????????
            return
        }
        OutputStreamWriter(file, "UTF-8").use { spec.writeTo(it) }
    }

    private fun extractFields(clazz: Class<*>): List<FieldInfo> {
        return clazz.declaredFields.mapNotNull { field ->
            FieldInfo(
                name = field.name,
                type = field.type,
                genericType = field.genericType,
                nullable = when {
                    field.type.isPrimitive -> false // javaのプリミティブは無条件でnon-null
                    field.type == java.util.List::class.java -> false
                    // TODO: どうにかしたい
                    clazz == Video::class.java && setOf("url", "previewUrl", "altContent").contains(field.name) -> false
                    field.isAnnotationPresent(Nullable::class.java) -> true
                    else -> true
                }
            )
        }
    }

    private fun generatePropertySpec(fieldInfo: FieldInfo): PropertySpec {
        val typeName = when {
            fieldInfo.type.isPrimitive -> {
                when (fieldInfo.type.simpleName) {
                    "boolean" -> BOOLEAN.copy(nullable = fieldInfo.nullable)
                    "byte" -> BYTE.copy(nullable = fieldInfo.nullable)
                    "char" -> CHAR.copy(nullable = fieldInfo.nullable)
                    "short" -> SHORT.copy(nullable = fieldInfo.nullable)
                    "int" -> INT.copy(nullable = fieldInfo.nullable)
                    "long" -> LONG.copy(nullable = fieldInfo.nullable)
                    "float" -> FLOAT.copy(nullable = fieldInfo.nullable)
                    "double" -> DOUBLE.copy(nullable = fieldInfo.nullable)
                    else -> throw IllegalArgumentException("Unsupported primitive type: ${fieldInfo.type}")
                }
            }

            fieldInfo.type == java.lang.Boolean::class.java -> BOOLEAN.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.Byte::class.java -> BYTE.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.Character::class.java -> CHAR.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.Short::class.java -> SHORT.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.Integer::class.java -> INT.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.Long::class.java -> LONG.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.Float::class.java -> FLOAT.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.Double::class.java -> DOUBLE.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.lang.String::class.java -> STRING.copy(nullable = fieldInfo.nullable)
            fieldInfo.type == java.util.List::class.java -> {
                val type = fieldInfo.genericType as java.lang.reflect.ParameterizedType
                val typeArgument = type.actualTypeArguments.first() as Class<*>
                val typeName = ClassName.bestGuess(typeArgument.canonicalName)
                LIST.plusParameter(typeName)
            }

            else -> ClassName.bestGuess(fieldInfo.type.canonicalName).copy(nullable = fieldInfo.nullable)
        }

        val initValue = when {
            fieldInfo.type.isPrimitive -> {
                when (fieldInfo.type.simpleName) {
                    "boolean" -> "false"
                    "byte" -> "0"
                    "char" -> "'\\u0000'"
                    "short" -> "0"
                    "int" -> "0"
                    "long" -> "0L"
                    "float" -> "0.0f"
                    "double" -> "0.0"
                    else -> throw IllegalArgumentException("Unsupported primitive type: ${fieldInfo.type}")
                }
            }

            !fieldInfo.nullable -> fieldInfo.name

            fieldInfo.type == java.util.List::class.java -> "emptyList()"
            else -> "null"
        }
        return PropertySpec.builder(fieldInfo.name, typeName)
            .mutable(fieldInfo.nullable)
            .initializer(initValue)
            .build()
    }

    data class FieldInfo(val name: String, val type: Class<*>, val genericType: Type, val nullable: Boolean)
}
