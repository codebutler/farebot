package com.codebutler.farebot.transit.registry

import com.codebutler.farebot.transit.registry.CardConstructorType.CONTEXT
import com.codebutler.farebot.transit.registry.CardConstructorType.DEFAULT
import com.codebutler.farebot.transit.registry.CardConstructorType.INVALID
import com.codebutler.farebot.transit.registry.annotations.TransitCard
import com.codebutler.farebot.transit.registry.annotations.TransitCardRegistry
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreElements.isAnnotationPresent
import com.google.auto.service.AutoService
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import com.uber.crumb.compiler.api.ConsumerMetadata
import com.uber.crumb.compiler.api.CrumbConsumerExtension
import com.uber.crumb.compiler.api.CrumbContext
import com.uber.crumb.compiler.api.CrumbProducerExtension
import com.uber.crumb.compiler.api.ProducerMetadata
import java.io.IOException
import java.util.Arrays
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic


@AutoService(CrumbProducerExtension::class, CrumbConsumerExtension::class)
class TransitRegistryCompiler : CrumbProducerExtension, CrumbConsumerExtension {

    companion object {
        const val METADATA_KEY = "TransitRegistryCompiler"
    }

    override fun key() = METADATA_KEY

    override fun supportedProducerAnnotations() = setOf(TransitCard::class.java)

    override fun supportedConsumerAnnotations() = setOf(TransitCardRegistry::class.java)

    override fun isProducerApplicable(context: CrumbContext,
            type: TypeElement,
            annotations: Collection<AnnotationMirror>): Boolean {
        return isAnnotationPresent(type, TransitCard::class.java)
    }

    override fun isConsumerApplicable(context: CrumbContext,
            type: TypeElement,
            annotations: Collection<AnnotationMirror>): Boolean {
        return isAnnotationPresent(type, TransitCardRegistry::class.java)
    }

    override fun produce(context: CrumbContext,
            type: TypeElement,
            annotations: Collection<AnnotationMirror>): ProducerMetadata {
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
                            "Must have a public default constructor or single Context parameter constructor to be usable in transit registries.",
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

    override fun consume(context: CrumbContext,
            type: TypeElement,
            annotations: Collection<AnnotationMirror>,
            metadata: Set<ConsumerMetadata>) {
        // Must be an abstract class because we're generating the backing implementation.
        if (type.kind != ElementKind.CLASS) {
            context.processingEnv
                    .messager
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "@${TransitCardRegistry::class.java.simpleName} is only applicable on classes when consuming!",
                            type)
            return
        } else if (ABSTRACT !in type.modifiers) {
            context.processingEnv
                    .messager
                    .printMessage(Diagnostic.Kind.ERROR, "Must be abstract!", type)
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

        val cardCn = ClassName.get("com.codebutler.farebot.card", "Card")
        val cardMapInitializer = CodeBlock.of("new \$T<>()", LinkedHashMap::class.java)
        val transitFactoryListType = ParameterizedTypeName.get(
                ClassName.bestGuess(FQCN_TRANSIT_FACTORY),
                cardCn,
                ClassName.get("com.codebutler.farebot.transit",
                        "TransitInfo")
        )
        val cardsMapType = ParameterizedTypeName.get(
                ClassName.get(Map::class.java),
                ParameterizedTypeName.get(ClassName.get(Class::class.java),
                        WildcardTypeName.subtypeOf(cardCn)),
                ParameterizedTypeName.get(ClassName.get(List::class.java), transitFactoryListType))


        val requiresContext = cardClasses
                .values
                .flatMap { it }.any { it.constructorType == CONTEXT }
        val contextParam = ParameterSpec.builder(ClassName.bestGuess(CONTEXT_FQCN),
                "context").build()
        val createMethod = MethodSpec.methodBuilder("create")
                .addModifiers(STATIC)
                .returns(cardsMapType)
                .apply {
                    if (requiresContext) {
                        addParameter(contextParam)
                    }
                }
                .addStatement("\$T registry = \$L", cardsMapType, cardMapInitializer)
                .apply {
                    cardClasses.forEach { (cardType, metas) ->
                        val elementsListTypes = metas.joinToString {
                            if (it.constructorType == DEFAULT) {
                                "\$T"
                            } else {
                                "\$T(\$N)"
                            }
                        }
                        val elementsList = metas
                                .flatMap {
                                    val target = ClassName.get(it.element)
                                    if (it.constructorType == DEFAULT) {
                                        setOf(target)
                                    } else {
                                        setOf(target, contextParam)
                                    }
                                }
                                .toTypedArray()
                        val elementsCodeBlock = CodeBlock.of(elementsListTypes, *elementsList)
                        addStatement("registry.put(\$T.class, \$T.asList(\$L))",
                                cardType,
                                ClassName.get(Arrays::class.java),
                                elementsCodeBlock
                        )
                    }
                }
                .addStatement("return registry")
                .build()

        val generatedClass = TypeSpec.classBuilder("CardsRegistry_" + type.simpleName.toString())
                .addModifiers(FINAL)
                .superclass(TypeName.get(type.asType()))
                .addMethod(createMethod)
                .build()

        try {
            JavaFile.builder(MoreElements.getPackage(type).qualifiedName.toString(), generatedClass)
                    .build()
                    .writeTo(context.processingEnv.filer)
        } catch (e: IOException) {
            context.processingEnv
                    .messager
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "Failed to write generated plugins mapping! ${e.message}",
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
 * https://github.com/rharter/auto-value-moshi/blob/c397632e87f65bd53ff890542b0ae9c0a93b8e3c/auto-value-moshi/src/main/java/com/ryanharter/auto/value/moshi/AutoValueMoshiAdapterFactoryProcessor.java#L255
 */
private fun TypeElement.transitFactoryInterface(): ParameterizedTypeName? {
    val targetTypeName = ClassName.bestGuess(FQCN_TRANSIT_FACTORY)
    for (iface in interfaces) {
        if (iface is DeclaredType) {
            val typename = TypeName.get(iface)
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
                    } else if (PUBLIC in it.modifiers
                            && it.parameters.size == 1
                            && it.parameters[0].asType().toString() == CONTEXT_FQCN) {
                        return CONTEXT
                    }
                }
        return DEFAULT
    }

private data class TransitCardCodegenMeta(val element: TypeElement, val cardType: TypeName) {
    val constructorType = element.constructorType
}
