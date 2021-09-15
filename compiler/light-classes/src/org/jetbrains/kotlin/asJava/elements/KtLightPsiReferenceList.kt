/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.addSuperTypeEntry
import org.jetbrains.kotlin.asJava.classes.findEntry
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

class KtLightPsiReferenceList(
    override val clsDelegate: PsiReferenceList,
    private val owner: KtLightClass
) : KtLightElement<KtSuperTypeList, PsiReferenceList>, PsiReferenceList by clsDelegate {
    inner class KtLightSuperTypeReference(
        override val clsDelegate: PsiJavaCodeReferenceElement
    ) : KtLightElement<KtSuperTypeListEntry, PsiJavaCodeReferenceElement>, PsiJavaCodeReferenceElement by clsDelegate {

        override val kotlinOrigin by lazyPub {
            clsDelegate.qualifiedName?.let { this@KtLightPsiReferenceList.kotlinOrigin?.findEntry(it) }
        }

        override fun getParent() = this@KtLightPsiReferenceList

        override fun delete() {
            val superTypeList = this@KtLightPsiReferenceList.kotlinOrigin ?: return
            val entry = kotlinOrigin ?: return
            superTypeList.removeEntry(entry)
        }

        override fun getTextRange(): TextRange? = kotlinOrigin?.typeReference?.textRange ?: TextRange.EMPTY_RANGE
    }

    override val kotlinOrigin: KtSuperTypeList?
        get() = owner.kotlinOrigin?.getSuperTypeList()

    private val _referenceElements by lazyPub {
        clsDelegate.referenceElements.map { KtLightSuperTypeReference(it) }.toTypedArray()
    }

    override fun getParent() = owner

    override fun getReferenceElements() = _referenceElements

    override fun add(element: PsiElement): PsiElement {
        if (element !is KtLightSuperTypeReference) throw UnsupportedOperationException("Unexpected element: ${element.getElementTextWithContext()}")

        val superTypeList = kotlinOrigin ?: return element
        val entry = element.kotlinOrigin ?: return element

        this.addSuperTypeEntry(superTypeList, entry, element)

        return element
    }
}
