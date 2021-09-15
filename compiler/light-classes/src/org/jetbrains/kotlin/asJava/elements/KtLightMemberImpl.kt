/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsRepositoryPsiElement
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory.ORIGIN
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.classes.isPossiblyAffectedByAllOpen
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.hasBody
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class KtLightMemberImpl<out D : PsiMember>(
    computeRealDelegate: () -> D,
    override val lightMemberOrigin: LightMemberOrigin?,
    private val containingClass: KtLightClass,
    protected val dummyDelegate: D?
) : KtLightElementBase(containingClass), PsiMember, KtLightMember<D> {
    override val clsDelegate by lazyPub(computeRealDelegate)
    private val lightIdentifier by lazyPub { KtLightIdentifier(this, kotlinOrigin as? KtNamedDeclaration) }

    private val _modifierList by lazyPub {
        if (lightMemberOrigin is LightMemberOriginForDeclaration)
            KtLightMemberModifierList(this, dummyDelegate?.modifierList)
        else
            clsDelegate.modifierList!!
    }

    override fun hasModifierProperty(name: String) = _modifierList.hasModifierProperty(name)

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun toString(): String = "${this::class.java.simpleName}:$name"

    override fun getContainingClass() = containingClass

    override fun getName(): String = dummyDelegate?.name ?: clsDelegate.name!!

    override fun getNameIdentifier(): PsiIdentifier = lightIdentifier

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun getDocComment() = (clsDelegate as PsiDocCommentOwner).docComment

    override fun isDeprecated() = (clsDelegate as PsiDocCommentOwner).isDeprecated

    override fun isValid(): Boolean = parent.isValid && lightMemberOrigin?.isValid() != false

    override fun isEquivalentTo(another: PsiElement?): Boolean = this == another ||
            lightMemberOrigin?.isEquivalentTo(another) == true ||
            another is KtLightMember<*> && lightMemberOrigin?.isEquivalentTo(another.lightMemberOrigin) == true
}

internal fun getMemberOrigin(member: PsiMember): LightMemberOriginForDeclaration? {
    if (member !is ClsRepositoryPsiElement<*>) return null

    return member.stub?.safeAs<UserDataHolder>()?.getUserData(ORIGIN) as? LightMemberOriginForDeclaration
}

private val visibilityModifiers = arrayOf(PsiModifier.PRIVATE, PsiModifier.PACKAGE_LOCAL, PsiModifier.PROTECTED, PsiModifier.PUBLIC)

private class KtLightMemberModifierList(owner: KtLightMember<*>, private val dummyDelegate: PsiModifierList?) :
    KtLightModifierList<KtLightMember<*>>(owner) {
    override fun hasModifierProperty(name: String) = when {
        name == PsiModifier.ABSTRACT && isImplementationInInterface() -> false
        // pretend this method behaves like a default method
        name == PsiModifier.DEFAULT && isImplementationInInterface() -> true
        name == PsiModifier.FINAL && owner.containingClass
            .safeAs<KtLightClassForSourceDeclaration>()
            ?.isPossiblyAffectedByAllOpen() == true -> clsDelegate.hasModifierProperty(name)

        dummyDelegate != null -> when {
            name in visibilityModifiers && isMethodOverride() -> clsDelegate.hasModifierProperty(name)
            else -> dummyDelegate.hasModifierProperty(name)
        }

        else -> clsDelegate.hasModifierProperty(name)
    }

    override fun hasExplicitModifier(name: String) =
        // kotlin methods can't be truly default atm, that way we can avoid being reported on by diagnostics, namely android lint
        if (name == PsiModifier.DEFAULT) false else super.hasExplicitModifier(name)

    private fun isMethodOverride() = owner is KtLightMethod && owner.kotlinOrigin?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

    private fun isImplementationInInterface() =
        owner.containingClass.isInterface && owner is KtLightMethod && owner.kotlinOrigin?.hasBody() ?: false

    override fun copy() = KtLightMemberModifierList(owner, dummyDelegate)
}

