/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.asJava.builder.LightClassBuilderResult
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator

typealias LightClassBuilder = (LightClassConstructionContext) -> LightClassBuilderResult

abstract class LightClassGenerationSupport {
    abstract fun createDataHolderForClass(classOrObject: KtClassOrObject, builder: LightClassBuilder): LightClassDataHolder.ForClass

    abstract fun createDataHolderForFacade(files: Collection<KtFile>, builder: LightClassBuilder): LightClassDataHolder.ForFacade

    abstract fun createDataHolderForScript(script: KtScript, builder: LightClassBuilder): LightClassDataHolder.ForScript

    abstract fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor?

    abstract fun analyze(element: KtElement): BindingContext

    abstract fun analyzeAnnotation(element: KtAnnotationEntry): AnnotationDescriptor?

    abstract fun analyzeWithContent(element: KtClassOrObject): BindingContext

    protected abstract fun getUltraLightClassSupport(element: KtElement): KtUltraLightSupport

    fun createConstantEvaluator(expression: KtExpression): ConstantExpressionEvaluator = getUltraLightClassSupport(expression).run {
        ConstantExpressionEvaluator(moduleDescriptor, languageVersionSettings, expression.project)
    }

    abstract val useUltraLightClasses: Boolean

    fun createUltraLightClassForFacade(
        manager: PsiManager,
        facadeClassFqName: FqName,
        lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade>,
        files: Collection<KtFile>,
    ): KtUltraLightClassForFacade? {

        if (!useUltraLightClasses) return null

        if (files.any { it.isScript() }) return null

        val filesToSupports: List<Pair<KtFile, KtUltraLightSupport>> = files.map {
            it to getUltraLightClassSupport(it)
        }

        return KtUltraLightClassForFacade(
            manager,
            facadeClassFqName,
            lightClassDataCache,
            files,
            filesToSupports
        )
    }

    fun createUltraLightClass(element: KtClassOrObject): KtUltraLightClass? {

        if (!useUltraLightClasses) return null

        if (element.shouldNotBeVisibleAsLightClass()) return null

        return getUltraLightClassSupport(element).let { support ->
            when {
                element is KtObjectDeclaration && element.isObjectLiteral() -> KtUltraLightClassForAnonymousDeclaration(element, support)
                element.safeIsLocal() -> KtUltraLightClassForLocalDeclaration(element, support)
                element.hasModifier(KtTokens.INLINE_KEYWORD) -> KtUltraLightInlineClass(element, support)
                else -> KtUltraLightClass(element, support)
            }
        }
    }

    fun createUltraLightClassForScript(script: KtScript): KtUltraLightClassForScript? =
        if (useUltraLightClasses) KtUltraLightClassForScript(script, support = getUltraLightClassSupport(script)) else null

    companion object {
        @JvmStatic
        fun getInstance(project: Project): LightClassGenerationSupport {
            return ServiceManager.getService(project, LightClassGenerationSupport::class.java)
        }
    }
}
