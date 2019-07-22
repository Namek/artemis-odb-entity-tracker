package net.namekdev.entity_tracker.utils


abstract class Nameable(var name: String = "")
fun <T : Nameable> T.named(name: String): T {
    this.name = name
    return this
}

abstract class Versionable(name: String = "") : Nameable(name) {
    internal var lastDataId = 0

    open fun invalidate() {
        lastDataId += 1
    }
}

class ValueContainer<T>(initialValue: T, var notifyChanged: (() -> Unit)? = null) : Versionable() {
    private var _value: T = initialValue

    var value: T
        get() = _value
        /**
         * Replace existing object. Notify about the change.
         */
        set(newValue) {
            _value = newValue
            invalidate()
            notifyChanged?.invoke()
        }

    /**
     * Update value of existing object. Notify about the change.
     */
    fun update(updateFun: (T) -> Unit) {
        updateFun(value)
        invalidate()
        notifyChanged?.invoke()
    }

    operator fun invoke() = value

    /**
     * Creates a transformation with cached result.
     */
    fun <R> cachedMap(fn: (T) -> R): ValueMapper<T, R> =
        ValueMapper(this, fn)
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


class RenderableValueMapper<T, R>(
    private val valueContainer: ValueContainer<T>,
    val renderMapFn: (RenderSession, T) -> R
) : Nameable() {
    private var lastDataId: Int = 1000
    var cachedResult: R? = null

    operator fun invoke(rendering: RenderSession): R {
        val dataId = valueContainer.lastDataId

        if (lastDataId != dataId) {
            rendering.invalidate()

            lastDataId = dataId
            rendering.push(valueContainer)
            cachedResult = renderMapFn(rendering, valueContainer.value)
            rendering.pop()
        }

        return cachedResult!!
    }
}

class RenderableValueMapper2<T1, T2, R>(
    private val valueContainer1: ValueContainer<T1>,
    private val valueContainer2: ValueContainer<T2>,
    val renderMapFn: (RenderSession, T1, T2) -> R
) : Nameable() {
    private var lastDataId1: Int = 1000
    private var lastDataId2: Int = 1000
    var cachedResult: R? = null

    operator fun invoke(rendering: RenderSession): R {
        val dataId1 = valueContainer1.lastDataId
        val dataId2 = valueContainer2.lastDataId

        if (lastDataId1 != dataId1 || lastDataId2 != dataId2) {
            rendering.invalidate()

            lastDataId1 = dataId1
            lastDataId2 = dataId2
            rendering.push(valueContainer1, valueContainer2)
            cachedResult = renderMapFn(rendering, valueContainer1.value, valueContainer2.value)
            rendering.pop()
        }

        return cachedResult!!
    }
}

fun <T1, T2, R> cachedMapMultiple(value1: ValueContainer<T1>, value2: ValueContainer<T2>, mapFn: (T1, T2) -> R): ValueMapper2<T1, T2, R> =
    ValueMapper2(value1, value2, mapFn)


fun <T, R> ValueContainer<T>.renderTo(fn: (RenderSession, T) -> R): RenderableValueMapper<T, R> =
    RenderableValueMapper(this, fn)

fun <T, R> renderTo(value: ValueContainer<T>, fn: (RenderSession, T) -> R): RenderableValueMapper<T, R> =
    RenderableValueMapper(value, fn)

fun <T1, T2, R> renderTo(value1: ValueContainer<T1>, value2: ValueContainer<T2>, mapFn: (RenderSession, T1, T2) -> R): RenderableValueMapper2<T1, T2, R> =
    RenderableValueMapper2(value1, value2, mapFn)



class RenderSession(rootVersionable: Versionable) {
    private val levels = mutableListOf<List<Versionable>>(listOf(rootVersionable))

    fun push(vararg v: Versionable) {
        levels.add(v.toList())
    }

    fun pop() {
        levels.removeAt(levels.size - 1)
    }

    fun invalidate() {
        for (l in levels)
            for (v in l) v.invalidate()
    }
}




// Example
typealias RenderData = String

class DataStore {
    val dataList = ValueContainer<MutableList<String>>(mutableListOf("a", "b", "c"))
}

class RootView : Versionable("RootView") {
    val subView = ValueContainer<SubView?>(null, ::requestRedraw)
    val dataStore = DataStore()


    fun onConnectedToServer() {
        subView.value = SubView(dataStore)
    }
    fun onAction123() {
        subView.update { it!!.counter += 1 }
    }
    fun requestRedraw() {
        // TODO call render() only when need to
        val rendering = RenderSession(this)
        val tree: String = render(rendering)
    }

    override fun invalidate() {
        super.invalidate()
        requestRedraw()
    }

    fun render(r: RenderSession) = subView.renderTo { r, subView ->
        (subView?.render?.invoke(r) ?: "") + renderNearStuff(r)
    }(r)

    val renderNearStuff = dataStore.dataList.renderTo { r, dataList ->
        dataList.joinToString()
    }
}

class SubView(val dataStore: DataStore) : Versionable() {
    var counter = 0
    val subSubOmg = SubSubView(dataStore)

    val render = dataStore.dataList.renderTo { r, dataList ->
        dataList.joinToString() + counter.toString() + " -> " + subSubOmg.render(r)
    }
}

class SubSubView(val dataStore: DataStore) : Versionable() {
    var subCounter = ValueContainer(100, ::invalidate)

    fun render(rendering: RenderSession): RenderData = renderTo(dataStore.dataList, subCounter) { r, dataList, subCounter ->
        dataList.size.toString() + " / $subCounter"
    }(rendering)

    fun onSomeClickAction() {
        subCounter.update { it + 10 }
    }
}
