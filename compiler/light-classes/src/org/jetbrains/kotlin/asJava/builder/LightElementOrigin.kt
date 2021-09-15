/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind


interface LightElementOrigin {
    val originalElement: PsiElement?
    val originKind: JvmDeclarationOriginKind?

    object None : LightElementOrigin {
        override val originalElement: PsiElement? get() = null
        override val originKind: JvmDeclarationOriginKind? get() = null
        override fun toString() = "NONE"
    }
}

fun JvmDeclarationOrigin.toLightMemberOrigin(): LightElementOrigin = when (val originalElement = element) {
    is KtAnnotationEntry -> DefaultLightElementOrigin(originalElement)
    is KtDeclaration -> LightMemberOriginForDeclaration(originalElement, originKind, parametersForJvmOverload)
    else -> LightElementOrigin.None
}

interface LightMemberOrigin : LightElementOrigin {
    override val originalElement: KtDeclaration?
    override val originKind: JvmDeclarationOriginKind
    val parametersForJvmOverloads: List<KtParameter?>? get() = null
    val auxiliaryOriginalElement: KtDeclaration? get() = null

    fun isValid(): Boolean

    fun isEquivalentTo(other: LightMemberOrigin?): Boolean
    fun isEquivalentTo(other: PsiElement?): Boolean

    fun copy(): LightMemberOrigin
}

data class LightMemberOriginForDeclaration(
    override val originalElement: KtDeclaration,
    override val originKind: JvmDeclarationOriginKind,
    override val parametersForJvmOverloads: List<KtParameter?>? = null,
    override val auxiliaryOriginalElement: KtDeclaration? = null
) : LightMemberOrigin {
    override fun isValid(): Boolean = originalElement.isValid

    override fun isEquivalentTo(other: LightMemberOrigin?): Boolean =
        if (other !is LightMemberOriginForDeclaration) false
        else isEquivalentTo(other.originalElement)

    override fun isEquivalentTo(other: PsiElement?): Boolean = originalElement.isEquivalentTo(other)

    override fun copy(): LightMemberOrigin =
        LightMemberOriginForDeclaration(originalElement.copy() as KtDeclaration, originKind, parametersForJvmOverloads)
}

data class DefaultLightElementOrigin(override val originalElement: PsiElement?) : LightElementOrigin {
    override val originKind: JvmDeclarationOriginKind? get() = null
}

fun PsiElement?.toLightClassOrigin(): LightElementOrigin = if (this != null) DefaultLightElementOrigin(this) else LightElementOrigin.None
