package net.namekdev.entity_tracker.utils



fun <T, R> ValueContainer<T>.cachedMap(fn: (T) -> R): ValueMapper<T, R> =
    ValueMapper(this, fn)

fun <T, R> cachedMap(value: ValueContainer<T>, fn: (T) -> R): ValueMapper<T, R> =
    ValueMapper(value, fn)

fun <T1, T2, R> cachedMap(value1: ValueContainer<T1>, value2: ValueContainer<T2>, mapFn: (T1, T2) -> R): ValueMapper2<T1, T2, R> =
    ValueMapper2(value1, value2, mapFn)


fun <T, R> ListenableValueContainer<T>.renderTo(fn: (RenderSession, T) -> R): RenderableValueMapper<T, R> =
    RenderableValueMapper(this, fn)

fun <T, R> renderTo(value: ListenableValueContainer<T>, fn: (RenderSession, T) -> R): RenderableValueMapper<T, R> =
    RenderableValueMapper(value, fn)

fun <T1, T2, R> renderTo(value1: ListenableValueContainer<T1>, value2: ListenableValueContainer<T2>, mapFn: (RenderSession, T1, T2) -> R): RenderableValueMapper2<T1, T2, R> =
    RenderableValueMapper2(value1, value2, mapFn)



abstract class Nameable(var name: String = "")
fun <T : Nameable> T.named(name: String): T {
    this.name = name
    return this
}

abstract class Versionable(name: String = "") : Nameable(name) {
    internal var lastVersion = 0

    open fun invalidate() {
        lastVersion += 1
    }
}

open class ValueContainer<T>(initialValue: T, var notifyChanged: (() -> Unit)? = null) : Versionable() {
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
}

class ListenableValueContainer<T>(initialValue: T) : ValueContainer<T>(initialValue) {
    val updateListeners: MutableList<() -> Unit> = mutableListOf()

    init {
        notifyChanged = {
            for (l in updateListeners)
                l.invoke()
        }
    }
}


interface Invalidable {
    fun invalidate()
}

abstract class BaseValueMapper<R>(private val argCount: Int) : Nameable(), Invalidable {
    private val lastVersion: Array<Int> = Array(argCount) { 1000 }
    var cachedResult: R? = null

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
            lastVersion[i] += 1
        }
    }
}

abstract class BaseRenderableValueMapper<R>(argCount: Int) : BaseValueMapper<R>(argCount) {
//    protected var latestRenderSession: RenderSession? = null
    protected var latestMapperAscendants: List<Invalidable> = listOf()

    protected fun registerTo(valueContainer: ListenableValueContainer<*>) {
        valueContainer.updateListeners.add {
//            latestRenderSession?.invalidateMapperWithAscendants(this)

//            latestRenderSession?.let {
//                for (l in it.mapLevels)
//                    l.invalidate()
//            }

            for (l in latestMapperAscendants)
                l.invalidate()
        }
    }
//
//    override fun invalidate() {
//        super.invalidate()
//    }
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
    private val valueContainer: ListenableValueContainer<T>,
    val renderMapFn: (RenderSession, T) -> R
) : BaseRenderableValueMapper<R>(1) {
    init {
        registerTo(valueContainer)
    }

    operator fun invoke(rendering: RenderSession): R {
//        latestRenderSession = rendering
        latestMapperAscendants = rendering.mapLevels.toList()
        if (needsRemap(valueContainer.lastVersion)) {
            rendering.push(this, valueContainer)
            cachedResult = renderMapFn(rendering, valueContainer.value)
            rendering.pop()
        }

        return cachedResult!!
    }
}

class RenderableValueMapper2<T1, T2, R>(
    private val valueContainer1: ListenableValueContainer<T1>,
    private val valueContainer2: ListenableValueContainer<T2>,
    val renderMapFn: (RenderSession, T1, T2) -> R
) : BaseRenderableValueMapper<R>(2) {
    init {
        // TODO well, it registers to it, so the values will definitely tell partial views about invalidation.
        //      However, RootView will not know about this :(
        // To fix this, we shouldn't store parents in RenderSession but here in the mapper.
        // Or, for a shortcut we could store the last RenderSession. Anyway, that's the only way
        // to call parent partials.

        // ^ Done that below but it's not enough. Eghh
        registerTo(valueContainer1)
        registerTo(valueContainer2)
    }

    operator fun invoke(rendering: RenderSession): R {
//        latestRenderSession = rendering
        latestMapperAscendants = rendering.mapLevels.toList()
        if (needsRemap(valueContainer1.lastVersion, valueContainer2.lastVersion)) {
            rendering.push(this, valueContainer1, valueContainer2)
            cachedResult = renderMapFn(rendering, valueContainer1.value, valueContainer2.value)
            rendering.pop()
        }

        return cachedResult!!
    }
}


class RenderSession(rootView: Invalidable) {
    val mapLevels = mutableListOf<Invalidable>(rootView)
    val couplings = mutableListOf<Coupling>()

    class Coupling(
        val mapper: Invalidable,
        val dependsOn: Array<out Versionable>,
        val levelAbove: Invalidable?
    )

    fun push(mapper: BaseValueMapper<*>, vararg v: Versionable) {
        couplings.add(Coupling(mapper, v, mapLevels.lastOrNull()))
        mapLevels.add(mapper)
    }

    fun pop() {
        mapLevels.removeAt(mapLevels.size - 1)
    }

    fun invalidateMapperWithAscendants(mapper: Invalidable) {
        mapper.invalidate()

        // invalidate ascendant mappers!
        var currentMapper: Invalidable? = mapper

        while (currentMapper != null) {
            var newMapper: Invalidable? = null

            for (coupling in couplings) {
                if (coupling.mapper === newMapper) {
                    coupling.levelAbove?.let {
                        it.invalidate()
                        newMapper = it
                    }
                }
            }
            currentMapper = newMapper

//            if (currentMapper == previousMapper)
//                currentMapper = null
        }
    }

//    fun <T> invalidateDependantsOfValueContainer(valueContainer: ListenableValueContainer<T>) {
//        for (coupling in couplings) {
//            if (coupling.dependsOn.contains(valueContainer)) {
//                val mapper = coupling.mapper
//                invalidateMapperWithAscendants(mapper)
//            }
//        }
//    }
}




// Example
data class RenderNode(val name: String, val nodes: List<RenderNode> = listOf())

class DataStore() {
    val dataList = ListenableValueContainer<MutableList<String>>(mutableListOf("a", "b", "c"))
}

class RootView : Invalidable {
    lateinit var latestRenderSession: RenderSession
    val latestRenderSessionGetter: () -> RenderSession = {
        latestRenderSession
    }

    val subView = ListenableValueContainer<SubView?>(null)
    val dataStore = DataStore()

    fun onConnectedToServer() {
        subView.value = SubView(dataStore)
    }
    fun onAction123() {
        subView.update { it!!.counter += 1 }
    }
    fun requestRedraw(): RenderNode {
        // TODO call render() only when need to
        latestRenderSession = RenderSession(this)
        val tree: RenderNode = render(latestRenderSession)

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

class SubView(val dataStore: DataStore) : Versionable() {
    var counter = 0
    val subSubOmg = SubSubView(dataStore)

    val render = dataStore.dataList.renderTo { r, dataList ->
        RenderNode("SubView.render", listOf(
            RenderNode(dataList.joinToString() + " -> " + counter.toString()),
            subSubOmg.render(r)
        ))
    }
}

class SubSubView(val dataStore: DataStore) : Versionable() {
    val subCounter = ListenableValueContainer(100)

    fun render(rendering: RenderSession): RenderNode = renderTo(dataStore.dataList, subCounter) { r, dataList, subCounter ->
        RenderNode("SubSubView.render", listOf(
            RenderNode(dataList.size.toString() + " / $subCounter")
        ))
    }(rendering)

    fun onSomeClickAction() {
        subCounter.update { it + 10 }
    }
}
