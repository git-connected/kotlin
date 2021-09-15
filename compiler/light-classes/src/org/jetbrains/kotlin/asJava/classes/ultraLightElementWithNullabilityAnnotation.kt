/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.nullability

interface KtUltraLightElementWithNullabilityAnnotation<out T : KtDeclaration, out D : PsiModifierListOwner> : KtLightDeclaration<T, D>,
    PsiModifierListOwner {

    fun computeQualifiedNameForNullabilityAnnotation(kotlinType: KotlinType?): String? {
        val notErrorKotlinType = kotlinType?.takeUnless(KotlinType::isError) ?: return null
        val psiType = psiTypeForNullabilityAnnotation ?: return null
        if (psiType is PsiPrimitiveType) return null

        return nullableName(notErrorKotlinType)
    }

    val qualifiedNameForNullabilityAnnotation: String?
    val psiTypeForNullabilityAnnotation: PsiType?

    companion object {
        fun nullableName(kotlinType: KotlinType): String? {
            if (kotlinType.isTypeParameter()) {
                if (!TypeUtils.hasNullableSuperType(kotlinType)) return NotNull::class.java.name
                if (!kotlinType.isMarkedNullable) return null
            }

            return when (kotlinType.nullability()) {
                TypeNullability.NOT_NULL -> NotNull::class.java.name
                TypeNullability.NULLABLE -> Nullable::class.java.name
                TypeNullability.FLEXIBLE -> null
            }
        }
    }
}