// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// IGNORE_DEXING

// WITH_RUNTIME
// !LANGUAGE: +InstantiationOfAnnotationClasses

// MODULE: lib
// FILE: lib.kt

annotation class A(val i: Int)

inline fun foo(i: Int): A = A(i)

inline fun bar(f: () -> Int): A = A(f())

class C {
    fun one(): A = foo(1)
    fun two(): A = bar { 2 }
}

fun box(): String {
    val one = C().one()
    val two = C().two()
    assert(one.i == 1)
    assert(two.i == 2)
    assert(one == A(1))
    assert(two == A(2))
    return "OK"
}