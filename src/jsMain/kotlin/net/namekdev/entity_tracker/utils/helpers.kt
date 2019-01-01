package net.namekdev.entity_tracker.utils


inline fun <T, R> Iterable<T>.mapToArray(transform: (T) -> R): Array<R> {
    return map(transform).toTypedArray()
}

inline fun <K, V, R> Map<out K, V>.mapToArray(transform: (Map.Entry<K, V>) -> R): Array<R> {
    return map(transform).toTypedArray()
}


abstract class MemoizerBase<T>(var lastHashCode: Int = 0) {
    abstract val value: T

    fun <R> transform(transformer: (T) -> R): TransformMemoizer<T, R> {
        return TransformMemoizer(this, transformer)
    }
}

fun <T1, T2, R> transformMultiple(memo1: MemoizerBase<T1>, memo2: MemoizerBase<T2>, transformer: (T1, T2) -> R): TransformMemoizer<Pair<T1, T2>, R> {
    fun getValues(): Pair<T1, T2> {
        return Pair(memo1.value, memo2.value)
    }

    fun t(pair: Pair<T1, T2>): R {
        val (v1, v2) = pair
        return transformer(v1, v2)
    }

    return TransformMemoizer(ExternalMemoizer(::getValues), ::t)
}

fun <T1, T2, T3, R> transformMultiple(memo1: MemoizerBase<T1>, memo2: MemoizerBase<T2>, memo3: MemoizerBase<T3>, transformer: (T1, T2, T3) -> R): TransformMemoizer<Triple<T1, T2, T3>, R> {
    fun getValues(): Triple<T1, T2, T3> {
        return Triple(memo1.value, memo2.value, memo3.value)
    }

    fun t(triple: Triple<T1, T2, T3>): R {
        val (v1, v2, v3) = triple
        return transformer(v1, v2, v3)
    }

    return TransformMemoizer(ExternalMemoizer(::getValues), ::t)
}


/**
 * This structure does not hold transformation result, only the last hash code is kept.
 */
class ExternalMemoizer<T>(val itemToCheck: () -> T) : MemoizerBase<T>(itemToCheck().hashCode()) {
    override inline val value: T
        get() = itemToCheck()

}

class MemoContainer<T>(private var _value: T) : MemoizerBase<T>() {
    override var value: T
        get() = _value
        set(newValue) {
            _value = newValue
            lastHashCode = 0
        }

    init {
        lastHashCode = _value.hashCode()
    }

    operator fun invoke(): T =
        _value

}

/**
 * Maps given object to another value.
 * Transform result is cached and invalidated when object's hash code changes.
 * To achieve this object use {@see MemoizerBase.transform()}.
 */
class TransformMemoizer<T, R>(
    val valueHolder: MemoizerBase<T>,
    val transformer: (T) -> R
) {
    private var cachedResult: R = transformer(valueHolder.value)

    operator fun invoke(): R {
        val value = valueHolder.value
        val hashCode = value.hashCode()

        if (hashCode != valueHolder.lastHashCode) {
            valueHolder.lastHashCode = hashCode
            cachedResult = transformer(value)
        }

        return cachedResult
    }
}
