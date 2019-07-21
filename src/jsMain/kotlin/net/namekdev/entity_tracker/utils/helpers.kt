package net.namekdev.entity_tracker.utils

import org.w3c.dom.Element
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty


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


class ValueContainer<T>(initialValue: T, var notifyChanged: (() -> Unit)? = null) {
    private var _value: T = initialValue
    internal var lastDataId = 0

    var value: T
        get() = _value
        set(newValue) {
            _value = newValue
            lastDataId += 1
            notifyChanged?.invoke()
        }

    /**
     * Update value of existing object.
     * Notify about the change.
     */
    fun update(updateFun: (T) -> Unit) {
        updateFun(value)
        lastDataId += 1
        notifyChanged?.invoke()
    }

    operator fun invoke() = value

    /**
     * Creates a transformation with cached result.
     */
    fun <R> map(fn: (T) -> R): ValueMapper<T, R> =
        ValueMapper(this, fn)

    fun <T2, R> mapWith(valueContainer2: ValueContainer<T2>, fn: (T, T2) -> R): ValueMapper2<T, T2, R> =
        ValueMapper2(this, valueContainer2, fn)
}

class ValueMapper<T, R>(
    private val valueContainer: ValueContainer<T>,
    val mapFn: (T) -> R
) {
    private var lastDataId: Int = 1000
    var cachedResult: R? = null

    operator fun invoke(): R {
        val dataId = valueContainer.lastDataId
        if (lastDataId != dataId) {
            lastDataId = dataId
            cachedResult = mapFn(valueContainer.value)
        }

        return cachedResult!!
    }
}

class ValueMapper2<T1, T2, R>(
    private val valueContainer1: ValueContainer<T1>,
    private val valueContainer2: ValueContainer<T2>,
    val mapFn: (T1, T2) -> R
) {
    private var lastDataId1: Int = 1000
    private var lastDataId2: Int = 1000
    var cachedResult: R? = null

    operator fun invoke(): R {
        val dataId1 = valueContainer1.lastDataId
        val dataId2 = valueContainer2.lastDataId
        val needsRemap = lastDataId1 != dataId1 || lastDataId2 != dataId2

        lastDataId1 = dataId1
        lastDataId2 = dataId2

        if (needsRemap)
            cachedResult = mapFn(valueContainer1.value, valueContainer2.value)

        return cachedResult!!
    }
}

fun <T1, T2, R> mapMultiple(value1: ValueContainer<T1>, value2: ValueContainer<T2>, mapFn: (T1, T2) -> R): ValueMapper2<T1, T2, R> =
    ValueMapper2(value1, value2, mapFn)


// Example
typealias RenderData = String

class DataStore {
    val dataList = ValueContainer<MutableList<String>>(mutableListOf("a", "b", "c"))
}

class Omg {
    val subOmg = ValueContainer<SubOmg?>(null, ::requestRedraw)
    val dataStore = DataStore()


    init {
        onConnectedToServer()
    }

    fun onConnectedToServer() {
        subOmg.value = SubOmg(dataStore, ::requestRedraw)
    }

    fun onAction123() {
        subOmg.update { it!!.counter += 1 }
    }

    fun requestRedraw() {
        // TODO call render() only when need to
        render()
    }

    fun render(): String = subOmg.map {
        it?.render() ?: ""
    }()
}

class SubOmg(val dataStore: DataStore, val notifyChanged: () -> Unit) {
    var counter = 0
    val subSubOmg = SubOmg(dataStore, notifyChanged)

    fun render(): String = dataStore.dataList.map { dataList ->
        dataList.joinToString() + counter.toString() + " -> " + subSubOmg.render()
    }()
}

class SubSubOmg(val dataStore: DataStore, val notifyChanged: () -> Unit) {
    var subCounter = ValueContainer(100, notifyChanged)

    fun render(): RenderData = mapMultiple(dataStore.dataList, subCounter) { dataList, subCounter ->
        dataList.size.toString() + " / $subCounter"
    }()

    fun onSomeClickAction() {
        subCounter.update { it + 10 }
        notifyChanged()
    }
}
