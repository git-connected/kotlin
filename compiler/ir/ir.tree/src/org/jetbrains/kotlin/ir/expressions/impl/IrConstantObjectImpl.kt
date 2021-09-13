/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.SmartList

class IrConstantPrimitiveImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var value: IrConst<*>,
) : IrConstantPrimitive() {
    override fun contentEquals(other: IrConstantValue) =
        other is IrConstantPrimitiveImpl &&
                value.kind == other.value.kind &&
                value.value == other.value

    override fun contentHashCode() =
        value.kind.hashCode() * 31 + value.value.hashCode()

    override var type = value.type

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        value.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        value = value.transform(transformer, data) as IrConst<*>
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstantPrimitive(this, data)
    }
}

class IrConstantObjectImpl constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override val constructor: IrConstructorSymbol,
    initArguments: List<IrConstantValue>,
    override val typeArguments: List<IrType>,
    override var type: IrType = constructor.owner.constructedClassType,
) : IrConstantObject() {
    override val arguments = SmartList(initArguments)

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstantObject(this, data)
    }

    override fun putArgument(index: Int, value: IrConstantValue) {
        arguments[index] = value
    }

    override fun contentEquals(other: IrConstantValue): Boolean =
        other is IrConstantObjectImpl &&
                other.type == type &&
                other.constructor == constructor &&
                arguments.size == other.arguments.size &&
                typeArguments.size == other.typeArguments.size &&
                arguments.indices.all { index -> arguments[index].contentEquals(other.arguments[index]) } &&
                typeArguments.indices.all { index -> typeArguments[index] == other.typeArguments[index] }


    override fun contentHashCode(): Int {
        var res = type.hashCode() * 31 + constructor.hashCode()
        for (value in arguments) {
            res = res * 31 + value.contentHashCode()
        }
        for (value in typeArguments) {
            res = res * 31 + value.hashCode()
        }
        return res
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        arguments.forEach { value -> value.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        arguments.transformInPlace { it.transform(transformer, data) }
    }
}

class IrConstantArrayImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    initElements: List<IrConstantValue>,
) : IrConstantArray() {
    override val elements = SmartList(initElements)
    override fun putElement(index: Int, value: IrConstantValue) {
        elements[index] = value
    }

    override fun contentEquals(other: IrConstantValue): Boolean =
        other is IrConstantArrayImpl &&
                other.type == type &&
                elements.size == other.elements.size &&
                elements.indices.all { elements[it].contentEquals(other.elements[it]) }

    override fun contentHashCode(): Int {
        var res = type.hashCode()
        for (value in elements) {
            res = res * 31 + value.contentHashCode()
        }
        return res
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstantArray(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        elements.forEach { value -> value.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        elements.transformInPlace { value -> value.transform(transformer, data) }
    }
}
