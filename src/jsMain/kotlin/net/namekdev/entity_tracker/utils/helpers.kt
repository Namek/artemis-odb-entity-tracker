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
//
//abstract class IChangeable {
//    internal var notificationReceivers: MutableList<() -> Unit> = mutableListOf()
//
//    fun notifyChanged() {
//        for (r in notificationReceivers)
//            r()
//    }
//}

fun createStyleElement(content: String): Element {
    val styleEl = document.createElement("style")
    styleEl.asDynamic().type = "text/css"
    styleEl.innerHTML = content
    document.getElementsByTagName("head")[0]!!.appendChild(styleEl)
    return styleEl
}
