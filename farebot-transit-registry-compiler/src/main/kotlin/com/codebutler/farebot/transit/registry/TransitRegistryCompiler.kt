package com.codebutler.farebot.transit.registry

import com.codebutler.farebot.transit.registry.CardConstructorType.CONTEXT
import com.codebutler.farebot.transit.registry.CardConstructorType.DEFAULT
import com.codebutler.farebot.transit.registry.CardConstructorType.INVALID
import com.codebutler.farebot.transit.registry.annotations.TransitCard
import com.codebutler.farebot.transit.registry.annotations.TransitCardRegistry
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreElements.isAnnotationPresent
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.uber.crumb.compiler.api.ConsumerMetadata
import com.uber.crumb.compiler.api.CrumbConsumerExtension
import com.uber.crumb.compiler.api.CrumbContext
import com.uber.crumb.compiler.api.CrumbProducerExtension
import com.uber.crumb.compiler.api.ProducerMetadata
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.classKind
import me.eugeniomarletti.kotlin.metadata.kaptGeneratedOption
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import org.jetbrains.kotlin.serialization.ProtoBuf.Class.Kind
import java.io.File
import java.io.IOException
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementKind.CLASS
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.ERROR

@AutoService(CrumbProducerExtension::class, CrumbConsumerExtension::class)
class TransitRegistryCompiler : CrumbProducerExtension, CrumbConsumerExtension {

    companion object {
        const val METADATA_KEY = "TransitRegistryCompiler"
    }

    override fun key() = METADATA_KEY

    override fun supportedProducerAnnotations() = setOf(TransitCard::class.java)

    override fun supportedConsumerAnnotations() = setOf(TransitCardRegistry::class.java)

    override fun isProducerApplicable(
        context: CrumbContext,
        type: TypeElement,
        annotations: Collection<AnnotationMirror>
    ): Boolean {
        return isAnnotationPresent(type, TransitCard::class.java)
    }

    override fun isConsumerApplicable(
        context: CrumbContext,
        type: TypeElement,
        annotations: Collection<AnnotationMirror>
    ): Boolean {
        return isAnnotationPresent(type, TransitCardRegistry::class.java)
    }

    override fun produce(
        context: CrumbContext,
        type: TypeElement,
        annotations: Collection<AnnotationMirror>
    ): ProducerMetadata {
        // Must be a class
        if (type.kind !== ElementKind.CLASS) {
            context.processingEnv
                    .messager
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "@${TransitCard::class.java.simpleName} is only applicable on classes!",
                            type)
            return mapOf()
        }

        // Must be instantiable (not abstract)
        if (type.modifiers.contains(ABSTRACT)) {
            context.processingEnv
                    .messager
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "@${TransitCard::class.java.simpleName} is not applicable on abstract classes!",
                            type)
            return mapOf()
        }

        // Check for valid constructor
        if (type.constructorType == INVALID) {
            context.processingEnv
                    .messager
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "Must have a public default constructor or single Context parameter constructor to be " +
                                    "usable in transit registries.",
                            type)
            return mapOf()
        }

        // Check implements TransitFactory
        if (type.transitFactoryInterface() == null) {
            context.processingEnv
                    .messager
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "Must implement TransitFactory!",
                            type)
            return mapOf()
        }

        return mapOf(METADATA_KEY to type.qualifiedName.toString())
    }

    override fun consume(
        context: CrumbContext,
        type: TypeElement,
        annotations: Collection<AnnotationMirror>,
        metadata: Set<ConsumerMetadata>
    ) {
        // Must be a type that supports extension functions
        if (type.kind != CLASS) {
            context.processingEnv
                    .messager
                    .printMessage(ERROR,
                            "@${TransitCardRegistry::class.java.simpleName} is only applicable on classes " +
                                    "when consuming!",
                            type)
            return
        }

        // Pull out the kotlin data
        val kmetadata = type.kotlinMetadata

        if (kmetadata !is KotlinClassMetadata) {
            context.processingEnv
                    .messager
                    .printMessage(ERROR,
                            "@${TransitCardRegistry::class.java.simpleName} can't be applied to $type: " +
                                    "must be a class.]",
                            type)
            return
        }

        val classData = kmetadata.data
        val (_, classProto) = classData

        // Must be an object class.
        if (classProto.classKind != Kind.OBJECT) {
            context.processingEnv
                    .messager
                    .printMessage(ERROR,
                            "@${TransitCardRegistry::class.java.simpleName} can't be applied to $type: must be a " +
                                    "Kotlin object class",
                            type)
            return
        }

        // List of card TypeElements by type
        val cardClasses = metadata
                .asSequence()
                .mapNotNull { it[METADATA_KEY] }
                .distinct()
                .sorted() // So the output is deterministic and cachable
                .map { cardClass ->
                    context.processingEnv.elementUtils.getTypeElement(cardClass)
                }
                .map { element ->
                    val transitFactoryType = element.transitFactoryInterface()

                    return@map if (transitFactoryType != null) {
                        val cardType = transitFactoryType.typeArguments[0]
                        TransitCardCodegenMeta(element, cardType)
                    } else {
                        context.processingEnv
                                .messager
                                .printMessage(Diagnostic.Kind.ERROR,
                                        "Type does not implement TransitFactory!",
                                        element)
                        null
                    }
                }
                .filterNotNull()
                .groupBy { it.cardType }

        val cardCn = ClassName("com.codebutler.farebot.card", "Card")
        val transitFactoryListType = ParameterizedTypeName.get(List::class.asClassName(),
                ParameterizedTypeName.get(
                        ClassName.bestGuess(FQCN_TRANSIT_FACTORY),
                        cardCn,
                        ClassName("com.codebutler.farebot.transit", "TransitInfo")
                ))
        val cardsMapType = ParameterizedTypeName.get(
                Map::class.asClassName(),
                ParameterizedTypeName.get(Class::class.asClassName(),
                        WildcardTypeName.subtypeOf(cardCn)),
                transitFactoryListType)

        val requiresContext = cardClasses
                .values
                .flatMap { it }
                .any { it.constructorType == CONTEXT }
        val contextParam = ParameterSpec.builder("context",
                ClassName.bestGuess(CONTEXT_FQCN)).build()
        // List of code blocks
        val cardClassesCodeBlocks = cardClasses
                .map { (cardType, metas) ->
                    val elementsListTypes = metas.joinToString {
                        if (it.constructorType == DEFAULT) {
                            "%T()"
                        } else {
                            "%T(%N)"
                        }
                    }
                    val elementsList = metas
                            .flatMap {
                                val target = it.element.asClassName()
                                if (it.constructorType == DEFAULT) {
                                    setOf(target)
                                } else {
                                    setOf(target, contextParam)
                                }
                            }
                            .toTypedArray()
                    val elementsCodeBlock = CodeBlock.of(elementsListTypes, *elementsList)
                    return@map CodeBlock.of("%T::class.java to listOf(%L) as %T",
                            cardType,
                            elementsCodeBlock,
                            transitFactoryListType)
                }
        val createFun = FunSpec.builder("create")
                .addAnnotation(AnnotationSpec.builder(Suppress::class)
                        .addMember("%S", "UNCHECKED_CAST")
                        .build())
                .receiver(type.asClassName())
                .returns(cardsMapType)
                .apply {
                    if (requiresContext) {
                        addParameter(contextParam)
                    }
                }
                .addStatement("return mapOf(\n${cardClassesCodeBlocks.joinToString(
                        ",\n") { "    %L" }}\n)",
                        *cardClassesCodeBlocks.toTypedArray())
                .build()

        try {
            // Generate the file
            val generatedDir = context.processingEnv.options[kaptGeneratedOption]?.let(::File)
                    ?: throw IllegalStateException("Could not resolve kotlin generated directory!")
            FileSpec.builder(MoreElements.getPackage(type).qualifiedName.toString(),
                    "Generated${type.simpleName}")
                    .addFunction(createFun)
                    .build()
                    .writeTo(generatedDir)
        } catch (e: IOException) {
            context.processingEnv
                    .messager
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "Failed to write generated registry! ${e.message}",
                            type)
        }
    }
}

private const val CONTEXT_FQCN = "android.content.Context"
private const val FQCN_TRANSIT_FACTORY = "com.codebutler.farebot.transit.TransitFactory"

private enum class CardConstructorType {
    DEFAULT, CONTEXT, INVALID
}

/**
 * Shallow interfaces check. If you want something more complex, do like here:
 * https://github.com/rharter/auto-value-moshi/blob/c397632e87f65bd53ff890542b0ae9c0a93b8e3c/auto-value-moshi/src
 * /main/java/com/ryanharter/auto/value/moshi/AutoValueMoshiAdapterFactoryProcessor.java#L255
 */
private fun TypeElement.transitFactoryInterface(): ParameterizedTypeName? {
    val targetTypeName = ClassName.bestGuess(FQCN_TRANSIT_FACTORY)
    for (iface in interfaces) {
        if (iface is DeclaredType) {
            val typename = iface.asTypeName()
            if (typename is ParameterizedTypeName && targetTypeName == typename.rawType) {
                return typename
            }
        }
    }
    return null
}

private val TypeElement.constructorType: CardConstructorType
    get() {
        ElementFilter.constructorsIn(enclosedElements)
                .forEach {
                    if (it.isDefault && PUBLIC !in it.modifiers) {
                        return INVALID
                    } else if (PUBLIC in it.modifiers &&
                            it.parameters.size == 1 &&
                            it.parameters[0].asType().toString() == CONTEXT_FQCN) {
                        return CONTEXT
                    }
                }
        return DEFAULT
    }

private data class TransitCardCodegenMeta(val element: TypeElement, val cardType: TypeName) {
    val constructorType = element.constructorType
}
