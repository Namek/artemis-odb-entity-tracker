package net.namekdev.entity_tracker.utils

import org.w3c.dom.Element
import org.w3c.dom.get
import kotlin.browser.document


inline fun <T, R> Iterable<T>.mapToArray(transform: (T) -> R): Array<R> {
    return map(transform).toTypedArray()
}

inline fun <K, V, R> Map<out K, V>.mapToArray(transform: (Map.Entry<K, V>) -> R): Array<R> {
    return map(transform).toTypedArray()
}

/**
 * Abstract value container that is able to to be transformed/mapped.
 */
abstract class Transformable<T>(open var lastHashCode: Int = 0) {
    abstract val value: T

    fun <R> transform(transformer: (T) -> R): ValueTransformer<T, R> {
        return ValueTransformer(this, transformer)
    }

    open operator fun invoke(): T =
        value
}

fun <T1, T2, R> transformMultiple(memo1: ValueContainer<T1>, memo2: ValueContainer<T2>, transformer: (T1, T2) -> R): ValueTransformer2<T1, T2, R> {
    return ValueTransformer2(memo1, memo2, transformer)
}

fun <T1, T2, T3, R> transformMultiple(memo1: Transformable<T1>, memo2: Transformable<T2>, memo3: Transformable<T3>, transformer: (T1, T2, T3) -> R): ValueTransformer<Triple<T1, T2, T3>, R> {
    fun getValues(): Triple<T1, T2, T3> {
        return Triple(memo1.value, memo2.value, memo3.value)
    }

    fun t(triple: Triple<T1, T2, T3>): R {
        val (v1, v2, v3) = triple
        return transformer(v1, v2, v3)
    }

    return ValueTransformer(ExternalMemoizer(::getValues), ::t)
}


/**
 * This structure does not hold transformation result, only the last hash code is kept.
 */
class ExternalMemoizer<T>(val itemToCheck: () -> T) : Transformable<T>(itemToCheck().hashCode()) {
    override inline val value: T
        get() = itemToCheck()
}

open class ValueContainer<T>(protected var _value: T) : Transformable<T>() {
    override var value: T
        get() = _value
        set(newValue) {
            _value = newValue
            lastHashCode = 0
        }

    init {
        lastHashCode = _value.hashCode()
    }
}

abstract class IChangeable {
    internal var notificationReceivers: MutableList<() -> Unit> = mutableListOf()

    fun notifyChanged() {
        for (r in notificationReceivers)
            r()
    }
}
class WatchableValueContainer<T : IChangeable?>(
    value: T,
    val onChangeDetected: () -> Unit = { }
) : ValueContainer<T>(value) {
    private var isDirty = true

    private val itChangedState = {
        isDirty = true
        lastHashCode = 0

        // this call is usually needed because otherwise the invoke() below wouldn't be called
        onChangeDetected()
    }

    override var value: T
        get() = _value
        set(newValue) {
            _value?.notificationReceivers?.remove(itChangedState)
            _value = newValue
            lastHashCode = 0
            _value?.notificationReceivers?.let { receivers ->
                if (!receivers.contains(itChangedState))
                    receivers.add(itChangedState)
            }
        }

    init {
        _value?.notificationReceivers?.let { receivers ->
            if (!receivers.contains(itChangedState))
                receivers.add(itChangedState)
        }
        lastHashCode = _value.hashCode()
    }

//    override operator fun invoke(): T {
//        if (isDirty) {
//            lastHashCode = 0
//            isDirty = false
//
//            onChangeDetected() // TODO seems like a duplicate call in some cases!
//        }
//
//        return value
//    }
}


/**
 * Maps a value to another value. Caches the transformation result.
 * Input value is invalidated when object's hash code changes,
 * so then a next invocation will transform again and cache it.
 * Invoke it to obtain the result value. Use {@see Transformable.transform(t)} for transformation.
 */
class ValueTransformer<T, R>(
    val valueContainer: Transformable<T>,
    val transformer: (T) -> R
) {
    var cachedResult: R? = null
    var lastHashCode = 0

    operator fun invoke(): R {
        val value = valueContainer.value
        val hashCode = value.hashCode()

        // lazy call
        val previousResult = cachedResult ?: transformer(value)

        if (hashCode != lastHashCode) {
            lastHashCode = hashCode
            cachedResult = transformer(value)
        }
        else if (cachedResult == null) {
            cachedResult = previousResult
        }

        return cachedResult!!
    }

    var name: String = ""
    fun named(name: String): ValueTransformer<T, R> {
        this.name = name
        return this
    }
}

class ValueTransformer2<T1, T2, R>(
    val valueContainer1: ValueContainer<T1>,
    val valueContainer2: ValueContainer<T2>,
    val transformer: (T1, T2) -> R
) {
    var cachedResult: R? = null
    private var lastHashCode1 = 0
    private var lastHashCode2 = 0

    operator fun invoke(): R {
        val value1 = valueContainer1()
        val value2 = valueContainer2()
        val hashCode1 = value1.hashCode()
        val hashCode2 = value2.hashCode()

        var needsTransforming = cachedResult == null

        if (hashCode1 != lastHashCode1) {
            lastHashCode1 = hashCode1
            needsTransforming = true
        }
        if (hashCode2 != lastHashCode2) {
            lastHashCode2 = hashCode2
            needsTransforming = true
        }

        if (needsTransforming) {
            cachedResult = transformer(value1, value2)
        }

        return cachedResult!!
    }
}

fun createStyleElement(content: String): Element {
    val styleEl = document.createElement("style")
    styleEl.asDynamic().type = "text/css"
    styleEl.innerHTML = content
    document.getElementsByTagName("head")[0]!!.appendChild(styleEl)
    return styleEl
}