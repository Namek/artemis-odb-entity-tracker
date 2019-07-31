package net.namekdev.entity_tracker.utils

import org.w3c.dom.Element
import org.w3c.dom.get
import kotlin.browser.document


inline fun <T, R> Iterable<T>.mapToArray(transform: (T) -> R): Array<R> {
    return map(transform).toTypedArray()
}

inline fun <T, R> Iterable<T>.filterMapTo(outCollection: MutableCollection<R>, transform: (T) -> R?): Iterable<R> {
    for (item in this) {
        val transformed = transform(item)
        if (transformed != null) {
            outCollection.add(transformed)
        }
    }

    return outCollection
}

inline fun <K, V, R> Map<out K, V>.mapToArray(transform: (Map.Entry<K, V>) -> R): Array<R> {
    return map(transform).toTypedArray()
}

inline fun <K, V, R> Map<out K, V>.filterMapTo(outCollection: MutableCollection<R>, transform: (Map.Entry<K, V>) -> R?): Iterable<R> {
    for (item in this) {
        val transformed = transform(item)
        if (transformed != null) {
            outCollection.add(transformed)
        }
    }

    return outCollection
}

fun createStyleElement(content: String): Element {
    val styleEl = document.createElement("style")
    styleEl.asDynamic().type = "text/css"
    styleEl.innerHTML = content
    document.getElementsByTagName("head")[0]!!.appendChild(styleEl)
    return styleEl
}

inline fun <reified T: Enum<T>> T.next(): T {
    val values = enumValues<T>()
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
}