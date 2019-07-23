package net.namekdev.entity_tracker.utils



fun <T, R> ValueContainer<T>.cachedMap(fn: (T) -> R): ValueMapper<T, R> =
    ValueMapper(this, fn)

fun <T, R> cachedMap(value: ValueContainer<T>, fn: (T) -> R): ValueMapper<T, R> =
    ValueMapper(value, fn)

fun <T1, T2, R> cachedMap(value1: ValueContainer<T1>, value2: ValueContainer<T2>, mapFn: (T1, T2) -> R): ValueMapper2<T1, T2, R> =
    ValueMapper2(value1, value2, mapFn)


fun <T, R> ValueContainer<T>.renderTo(fn: (RenderSession, T) -> R): RenderableValueMapper<T, R> =
    RenderableValueMapper(this, fn)

fun <T, R> renderTo(value: ValueContainer<T>, fn: (RenderSession, T) -> R): RenderableValueMapper<T, R> =
    RenderableValueMapper(value, fn)

fun <T1, T2, R> renderTo(value1: ValueContainer<T1>, value2: ValueContainer<T2>, mapFn: (RenderSession, T1, T2) -> R): RenderableValueMapper2<T1, T2, R> =
    RenderableValueMapper2(value1, value2, mapFn)



abstract class Nameable(var name: String = "")
fun <T : Nameable> T.named(name: String): T {
    this.name = name
    return this
}

interface Invalidable {
    fun invalidate()
}


open class ValueContainer<T>(initialValue: T) : Nameable() {
    val updateListeners: MutableList<() -> Unit> = mutableListOf()
    
    private var _value: T = initialValue
    internal var lastVersion = 0

    var value: T
        get() = _value
        /**
         * Replace existing object. Notify about the change.
         */
        set(newValue) {
            _value = newValue
            invalidate()
            for (l in updateListeners)
                l.invoke()
        }

    /**
     * Update value of existing object. Notify about the change.
     */
    fun update(updateFun: (T) -> Unit) {
        updateFun(value)
        invalidate()
        for (l in updateListeners)
            l.invoke()
    }

    private inline fun invalidate() {
        lastVersion += 1
    }

    operator fun invoke() = value
}


abstract class BaseValueMapper<R>(private val argCount: Int) : Nameable(), Invalidable {
    private val lastVersion: Array<Int> = Array(argCount) { 1000 }
    protected var cachedResult: R? = null

    protected fun needsRemap(vararg versions: Int): Boolean {
        var needsRemap = false
        for (i in 0 until argCount) {
            val v = versions[i]
            if (v != lastVersion[i]) {
                lastVersion[i] = v
                needsRemap = true
            }
        }
        return needsRemap
    }

    override fun invalidate() {
        for (i in 0 until argCount) {
            // note: feels like a hack since the ValueContainers were not changed but only this mapper needs to be refreshed
            lastVersion[i] -= 1
        }
    }
}

abstract class BaseRenderableValueMapper<R>(argCount: Int) : BaseValueMapper<R>(argCount) {
    protected var latestAscendants: List<Invalidable> = listOf()

    protected fun registerTo(valueContainer: ValueContainer<*>) {
        valueContainer.updateListeners.add {
            for (l in latestAscendants)
                l.invalidate()
        }
    }
}

class ValueMapper<T, R>(
    private val valueContainer: ValueContainer<T>,
    val mapFn: (T) -> R
) : BaseValueMapper<R>(1) {
    operator fun invoke(): R {
        if (needsRemap(valueContainer.lastVersion)) {
            cachedResult = mapFn(valueContainer.value)
        }

        return cachedResult!!
    }
}

class ValueMapper2<T1, T2, R>(
    private val valueContainer1: ValueContainer<T1>,
    private val valueContainer2: ValueContainer<T2>,
    val mapFn: (T1, T2) -> R
) : BaseValueMapper<R>(2) {
    operator fun invoke(): R {
        if (needsRemap(valueContainer1.lastVersion, valueContainer2.lastVersion))
            cachedResult = mapFn(valueContainer1.value, valueContainer2.value)

        return cachedResult!!
    }
}


class RenderableValueMapper<T, R>(
    private val valueContainer: ValueContainer<T>,
    val renderMapFn: (RenderSession, T) -> R
) : BaseRenderableValueMapper<R>(1) {
    init {
        registerTo(valueContainer)
    }

    operator fun invoke(rendering: RenderSession): R {
        latestAscendants = rendering.mapLevels.toList()

        if (needsRemap(valueContainer.lastVersion)) {
            rendering.push(this)
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
) : BaseRenderableValueMapper<R>(2) {
    init {
        registerTo(valueContainer1)
        registerTo(valueContainer2)
    }

    operator fun invoke(rendering: RenderSession): R {
        latestAscendants = rendering.mapLevels.toList()

        if (needsRemap(valueContainer1.lastVersion, valueContainer2.lastVersion)) {
            rendering.push(this)
            cachedResult = renderMapFn(rendering, valueContainer1.value, valueContainer2.value)
            rendering.pop()
        }

        return cachedResult!!
    }
}


class RenderSession(rootView: Invalidable) {
    val mapLevels = mutableListOf<Invalidable>(rootView)

    fun push(mapper: BaseValueMapper<*>) {
        mapLevels.add(mapper)
    }

    fun pop() {
        mapLevels.removeAt(mapLevels.size - 1)
    }
}




// Example
data class RenderNode(val name: String, val nodes: List<RenderNode> = listOf())

class DataStore() {
    val dataList = ValueContainer<MutableList<String>>(mutableListOf("a", "b", "c"))
}

class RootView : Invalidable {
    val subView = ValueContainer<SubView?>(null)
    val dataStore = DataStore()

    fun onConnectedToServer() {
        subView.value = SubView(dataStore)
    }
    fun onAction123() {
        subView.update { it!!.counter += 1 }
    }
    fun requestRedraw(): RenderNode {
        // TODO call render() only when need to
        val rendering= RenderSession(this)
        val tree: RenderNode = render(rendering)

        return tree
    }

    override fun invalidate() {
        requestRedraw()
    }

    fun render(r: RenderSession) = subView.renderTo { r, subView ->
        RenderNode("RootView.render", listOf(
            subView?.render?.invoke(r) ?: RenderNode("subView not existing"),
            renderNearStuff(r)
        ))
    }(r)

    val renderNearStuff = dataStore.dataList.renderTo { r, dataList ->
        RenderNode("RootView.renderNearStuff", listOf(
            RenderNode(dataList.joinToString())
        ))
    }
}

class SubView(val dataStore: DataStore) {
    var counter = 0
    val subSubOmg = SubSubView(dataStore)

    val render = dataStore.dataList.renderTo { r, dataList ->
        RenderNode("SubView.render", listOf(
            RenderNode(dataList.joinToString() + " -> " + counter.toString()),
            subSubOmg.render(r)
        ))
    }
}

class SubSubView(val dataStore: DataStore) {
    val subCounter = ValueContainer(100)

    fun render(rendering: RenderSession): RenderNode = renderTo(dataStore.dataList, subCounter) { r, dataList, subCounter ->
        RenderNode("SubSubView.render", listOf(
            RenderNode(dataList.size.toString() + " / $subCounter")
        ))
    }(rendering)

    fun onSomeClickAction() {
        subCounter.update { it + 10 }
    }
}
