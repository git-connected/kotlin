/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.light.AbstractLightClass
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.classes.KotlinClassInnerStuffCache.Companion.processDeclarationsInEnum
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.idea.KotlinLanguage

abstract class KtLightClassBase protected constructor(manager: PsiManager) : AbstractLightClass(manager, KotlinLanguage.INSTANCE),
    KtLightClass, PsiExtensibleClass {
    protected open val myInnersCache = KotlinClassInnerStuffCache(
        myClass = this,
        externalDependencies = listOf(KotlinModificationTrackerService.getInstance(manager.project).outOfBlockModificationTracker),
        lazyCreator = LightClassesLazyCreator(project)
    )

    override fun getDelegate() = clsDelegate

    override fun getFields() = myInnersCache.fields

    override fun getMethods() = myInnersCache.methods

    override fun getConstructors() = myInnersCache.constructors

    override fun getInnerClasses() = myInnersCache.innerClasses

    override fun getAllFields() = PsiClassImplUtil.getAllFields(this)

    override fun getAllMethods() = PsiClassImplUtil.getAllMethods(this)

    override fun getAllInnerClasses() = PsiClassImplUtil.getAllInnerClasses(this)

    override fun findFieldByName(name: String, checkBases: Boolean) = myInnersCache.findFieldByName(name, checkBases)

    override fun findMethodsByName(name: String, checkBases: Boolean) = myInnersCache.findMethodsByName(name, checkBases)

    override fun findInnerClassByName(name: String, checkBases: Boolean) = myInnersCache.findInnerClassByName(name, checkBases)

    override fun getOwnFields(): List<PsiField> = KtLightFieldImpl.fromClsFields(delegate, this)

    override fun getOwnMethods(): List<PsiMethod> = KtLightMethodImpl.fromClsMethods(delegate, this)

    override fun processDeclarations(
        processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement
    ): Boolean {
        if (isEnum) {
            if (!processDeclarationsInEnum(processor, state, myInnersCache)) return false
        }

        return super.processDeclarations(processor, state, lastParent, place)
    }

    override fun getText(): String = kotlinOrigin?.text ?: ""

    override fun getLanguage() = KotlinLanguage.INSTANCE

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun getContext() = parent

    override fun isEquivalentTo(another: PsiElement?): Boolean = PsiClassImplUtil.isClassEquivalentTo(this, another)
}
