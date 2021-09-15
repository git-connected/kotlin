/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiMember
import com.intellij.psi.StubBasedPsiElement

data class MemberIndex(private val index: Int) {
    companion object {
        @JvmField
        val KEY = Key.create<MemberIndex>("MEMBER_INDEX")
    }
}

val PsiMember.memberIndex: MemberIndex? get() = ((this as? StubBasedPsiElement<*>)?.stub as? UserDataHolder)?.getUserData(MemberIndex.KEY)