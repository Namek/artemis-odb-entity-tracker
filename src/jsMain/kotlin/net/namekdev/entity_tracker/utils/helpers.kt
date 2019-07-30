package net.namekdev.entity_tracker.utils

import org.w3c.dom.Element
import org.w3c.dom.get
import kotlin.browser.document


inline fun <T, R> Iterable<T>.mapToArray(transform: (T) -> R): Array<R> {
    return map(transform).toTypedArray()
}

inline fun <T, R> Iterable<T>.filterMapToArray(transform: (T) -> R?): Array<R> {
    val list = mutableListOf<R>()

    for (item in this) {
        val transformed = transform(item)
        if (transformed != null) {
            list.add(transformed)
        }
    }

    return list.toTypedArray()
}

inline fun <K, V, R> Map<out K, V>.mapToArray(transform: (Map.Entry<K, V>) -> R): Array<R> {
    return map(transform).toTypedArray()
}

inline fun <K, V, R> Map<out K, V>.filterMapToArray(transform: (Map.Entry<K, V>) -> R?): Array<R> {
    val list = mutableListOf<R>()

    for (item in this) {
        val transformed = transform(item)
        if (transformed != null) {
            list.add(transformed)
        }
    }

    return list.toTypedArray()
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