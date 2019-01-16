package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateInterfaceListener
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener.Companion.ENTITY_ADDED
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener.Companion.ENTITY_DELETED
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener.Companion.ENTITY_SYSTEM_STATS
import net.namekdev.entity_tracker.model.AspectInfo_Common
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.SystemInfo_Common
import net.namekdev.entity_tracker.network.ExternalInterfaceCommunicator
import net.namekdev.entity_tracker.network.WebSocketClient
import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.ui.Classes
import net.namekdev.entity_tracker.utils.*
import net.namekdev.entity_tracker.utils.serialization.DataType
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ValueTree
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import snabbdom.modules.*
import snabbdom.*
import kotlin.browser.document
import kotlin.browser.window


fun main(args: Array<String>) {
    window.onload = {
        createStyleElement(globalStylesheet)

        val rootEl = document.createElement("div") as HTMLElement
        arrayOf(Classes.root, Classes.any, Classes.single).forEach { rootEl.classList.add(it) }
        document.body!!.appendChild(rootEl)

        val container = document.createElement("div") as HTMLElement
        rootEl.appendChild(container)

        Main(container)
    }
}


typealias SystemInfo = SystemInfo_Common<CommonBitVector>
typealias AspectInfo = AspectInfo_Common<CommonBitVector>
data class CurrentComponent(val entityId: Int, val componentIndex: Int, val valueTree: ValueTree)

class ECSModel {
    val entityComponents = MemoContainer(mutableMapOf<Int, CommonBitVector>())
    val componentTypes = MemoContainer(mutableListOf<ComponentTypeInfo>())
    val allSystems = mutableListOf<SystemInfo>()
    val allManagersNames = mutableListOf<String>()
    val observedEntity = MemoContainer<Int?>(null)
    val currentComponent = MemoContainer<CurrentComponent?>(null)


    fun setComponentType(index: Int, info: ComponentTypeInfo) {
        componentTypes().add(index, info)
    }

    fun addEntity(entityId: Int, components: CommonBitVector) {
        entityComponents()[entityId] = components
    }

    fun removeEntity(entityId: Int) {
        entityComponents().remove(entityId)
    }

    fun getEntityComponents(entityId: Int): CommonBitVector =
        entityComponents()[entityId]!!


    fun getComponentTypeInfo(index: Int): ComponentTypeInfo =
        componentTypes().get(index)

    fun clear() {
        componentTypes().clear()
        entityComponents().clear()
    }
}

class Main(container: HTMLElement) : IWorldUpdateInterfaceListener<CommonBitVector> {
    val patch = Snabbdom.init(
        arrayOf(
            ClassModule(),
            AttributesModule(),
            PropsModule(),
            StyleModule(),
            EventListenersModule(),
            DatasetModule()
        )
    )
    lateinit var lastVnode: VNode
    var dynamicStyles: Element

    var demoStep = 0
    val entities = ECSModel()

    var worldController: IWorldController? = null
    var client: WebSocketClient? = null


    init {
        dynamicStyles = createStyleElement("")

        // due to JS compilation - view() can't be called before fields are initialized, so delay it's first execution
        window.setTimeout({
            lastVnode = patch(container, h("div"))
        }, 0)

        fun update() {
//            lastVnode = patch(lastVnode, view())
//            window.setTimeout({
//                update()
//            }, 50)
        }
//        update()

        client = WebSocketClient(ExternalInterfaceCommunicator(this))
        client!!.connect("ws://localhost:8025/actions")
    }

    val opts = OptionRecord(HoverSetting.AllowHover, FocusStyle())

    fun renderView() {
        val ctx = view()
        ctx.stylesheet?.let {
            dynamicStyles.innerHTML = toStyleSheetString(opts, it.values)
        }
        lastVnode = patch(lastVnode, ctx.vnode)

        console.log("update")
    }

    override fun disconnected() {
        entities.clear()
    }

    override fun injectWorldController(controller: IWorldController) {
        worldController = controller
    }

    override val listeningBitset: Int
        get() = ENTITY_ADDED or ENTITY_DELETED or ENTITY_SYSTEM_STATS

    override fun addedSystem(
        index: Int,
        name: String,
        allTypes: CommonBitVector?,
        oneTypes: CommonBitVector?,
        notTypes: CommonBitVector?
    ) {
        val aspectInfo = AspectInfo(allTypes, oneTypes, notTypes)
        val actives = if (aspectInfo.isEmpty) null else CommonBitVector()
        val systemInfo = SystemInfo(index, name, aspectInfo, actives)

        entities.allSystems.add(index, systemInfo)
        notifyUpdate()
    }

    override fun addedManager(name: String) {
        entities.allManagersNames.add(name)
        notifyUpdate()
    }

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
        entities.setComponentType(index, info)
        notifyUpdate()
    }

    override fun updatedEntitySystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int) {
        val system = entities.allSystems[systemIndex]
        system.entitiesCount = entitiesCount
        system.maxEntitiesCount = maxEntitiesCount
        notifyUpdate()
    }

    override fun addedEntity(entityId: Int, components: CommonBitVector) {
        entities.addEntity(entityId, components)
        notifyUpdate()
    }

    override fun deletedEntity(entityId: Int) {
        entities.removeEntity(entityId)
        notifyUpdate()
    }


    /**
     * Received after component value request was sent.
     */
    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
        entities.currentComponent.value = CurrentComponent(entityId, componentIndex, valueTree as ValueTree)
        notifyUpdate()
    }

    fun notifyUpdate() {
        // TODO start a very short timer that will wait for more updates

        // timeout is because of JS compilation - we have lateinit vars!
        window.setTimeout({
            renderView()
        }, 0)
    }

    fun view() =
        column(attrs(widthFill, heightFill, paddingXY(10, 10), spacing(10)),
            viewEntitiesTable(),
            viewEntitiesFilters(),
            row(attrs(widthFill),
                viewCurrentEntity(),
                viewSystems())
        )

    val viewEntitiesTable = transformMultiple(entities.entityComponents, entities.componentTypes) { entityComponents, componentTypes ->
        val idCol = thCell(row(attrs(paddingRight(15)), text("id")))
        val componentCols = componentTypes.mapToArray {
            thCell(row(attrs(paddingRight(15)), text(it.name)))
        }
        val entitiesDataRows = entityComponents.mapToArray { (entityId, components) ->
            val entityComponents = componentTypes.indices.mapToArray { cmpIndex ->
                if (components[cmpIndex])
                    tCell(
                        row(
                            attrs(
                                widthFill,
                                Attribute.Events(j("click" to {showComponent(entityId, cmpIndex)}))
                            ),
                            text("x")
                        )
                    )
                else tCell("")
            }

            tRow(tCell(entityId.toString()), *entityComponents)
        }

        val header = tRow(idCol, *componentCols)

        table(attrs(), header, *entitiesDataRows)
    }

    fun showComponent(entityId: Int, componentIndex: Int) {
        entities.observedEntity.value = entityId
        notifyUpdate()
        worldController?.requestComponentState(entityId, componentIndex)
    }

    fun viewEntitiesFilters() =
        row(arrayOf(text("TODO filters here?")))

    fun viewSystems(): RNode {
        // TODO checkboxes: entity systems, base systems (empty aspectInfo), managers (actives == null)

        val header = tRow(thCell(""), thCell("system"), thCell("entities"), thCell("max entities"))
        val rows = entities.allSystems
//            .filter { it.hasAspect }
            .mapToArray {
                tRow(
                    tCell(""),
                    tCell(it.name),
                    tCell(it.entitiesCount.toString()),
                    tCell(it.maxEntitiesCount.toString())
                )
            }
        return table(attrs(width(fill), alignTop), header, *rows)
    }

    val viewCurrentEntity = transformMultiple(entities.observedEntity, entities.currentComponent) { entityId, currentComponent ->
        if (entityId == null) {
            row(
                attrs(width(fillPortion(2)), heightFill),
                row(attrs(widthShrink, centerX), text("Please select any entity..."))
            )
        }
        else {
            val componentName = currentComponent?.componentIndex?.let {
                entities.componentTypes.value[it].name
            }
            row(
                attrs(width(fillPortion(2)), heightFill, spacing(25)),

                column(attrs(alignTop, heightFill, spacing(15)),
                    row(attrs(borderBottom(1)),
                        elems(text("Entity #$entityId"))),
                    viewObservedEntity()),
                column(attrs(widthFill, alignTop, spacing(6)),
                    row(attrs(borderBottom(0)),
                        elems(text(componentName?.let {"<$it>:"} ?: "" ))),
                    column(attrs(paddingLeft(12)), viewSelectedComponent()))
            )
        }
    }

    val viewObservedEntity = transformMultiple(entities.observedEntity, entities.currentComponent) { entityId, currentComponent ->
        val componentTypes = entities.entityComponents.value[entityId!!]
        if (componentTypes == null)
            column(elems(
                text("error: component types for entity #$entityId were not found")
            ))
        else {
            val componentNames = mutableListOf<RNode>()
            var i: Int = componentTypes.nextSetBit(0)
            while (i >= 0) {
                val cmpType = entities.componentTypes.value[i]
                val isSelected = cmpType.index == currentComponent?.componentIndex

                componentNames.add(
                    row(
                        attrs(
                            attrWhen(isSelected, backgroundColor(hexToColor(0xCFD8DC))),
                            Attribute.Events(j("click" to {showComponent(entityId, cmpType.index)}))
                        ),
                        text(cmpType.name)
                    )
                )

                i = componentTypes.nextSetBit(i+1)
            }

            column(
                attrs(spacing(5)),
                componentNames.toTypedArray()
            )
        }
    }

    val viewSelectedComponent = entities.currentComponent.transform { cmp ->
        if (cmp == null)
            column(arrayOf(text("")))
        else {
            viewValueTree(cmp.valueTree.model!!, cmp.valueTree, cmp.valueTree, listOf(cmp.entityId, cmp.componentIndex))
        }
    }

    fun viewValueTree(model: ObjectModelNode, value: Any?, rootValue: ValueTree, path: List<Int> = listOf(), level: Int = 0): RNode {
        return if (model.isArray) {
            // TODO value is ValueTree

            if (model.isEnumArray) {
                text("enum array!")
            }
            else {
//                if (model.isSubTypePrimitive)
                text("some array!")
            }
        }
        else if (model.isLeaf) {
            if (model.isEnum) {
                val enumDescription = model.children!![0]
                val enumTypeName = enumDescription.name!!
                val enumValuesNames = enumDescription.children!!.map { it.name!! }

                row(attrs(),
                    dataTypeToIcon(DataType.Enum, false),
                    text("${model.name ?: ""}<$enumTypeName> = "),
                    dropdown(value as Int?, enumValuesNames, true) {
                        onValueChanged(rootValue, path, model.dataType, it)
                    }
                )
            }
            else if (model.dataType == DataType.Boolean) {
                row(attrs(),
                    dataTypeToIcon(DataType.Boolean, model.isTypePrimitive),
                    text("${model.name ?: ""} ="),
                    checkbox(value as Boolean?, !model.isTypePrimitive) {
                        onValueChanged(rootValue, path, model.dataType, it)
                    }
                )
            }
            else {
                val inputType = when (model.dataType) {
                    DataType.Byte,
                    DataType.Short,
                    DataType.Int,
                    DataType.Long ->
                        InputType.Integer

                    DataType.Float,
                    DataType.Double ->
                        InputType.FloatingPointNumber

                    else -> InputType.Text
                }
                row(attrs(),
                    dataTypeToIcon(model.dataType, model.isTypePrimitive),
                    text("${model.name ?: ""} = "),
                    input(value?.let{it.toString()} ?: "<null>", inputType, !model.isTypePrimitive) {
                        val newValue: Any? = when (it) {
                            null -> null
                            is InputValueText -> it.text
                            is InputValueFloatingPoint -> {
                                when (model.dataType) {
                                    DataType.Float -> it.number.toFloat()
                                    DataType.Double -> it.number.toDouble()
                                    else ->
                                        throw RuntimeException("input() floating point should be only Float/Double and it is: ${model.dataType}")
                                }
                            }
                            is InputValueInteger -> {
                                when (model.dataType) {
                                    DataType.Byte -> it.number.toByte()
                                    DataType.Short -> it.number.toShort()
                                    DataType.Int -> it.number.toInt()
                                    DataType.Long -> it.number.toLong()
                                    else ->
                                        throw RuntimeException("integer was supposed to be Byte/Short/Int/Long and it is: ${model.dataType}")
                                }
                            }
                        }
                        onValueChanged(rootValue, path, model.dataType, newValue)
                    }
                )
            }
        }
        else {
            val vt = value as ValueTree
            val fields = model.children!!
                .mapIndexed { i, fieldModel ->
                    val fieldValue = vt.values[i]
                    viewValueTree(fieldModel, fieldValue, rootValue, path + i, level + 1)
                }

            if (level > 0)
                column(attrs(spacing(2)),
                    row(elems(
                        dataTypeToIcon(model.dataType, false),
                        text("${model.name ?: ""}:")
                    )),
                    column(attrs(paddingLeft(12)), fields.toTypedArray())
                )
            else
                column(attrs(spacing(6)), fields.toTypedArray())
        }
    }

    fun onValueChanged(rootValue: ValueTree, path: List<Int>, newValueType: DataType, newValue: Any?) {
        val entityId: Int = path[0]
        val componentIndex: Int = path[1]
        worldController!!.setComponentFieldValue(entityId, componentIndex, path.subList(2, path.size).toIntArray(), newValueType, newValue)

        // TODO this is just a workaround, we should modify the value in our model instead of refreshing state of whole component. (?)
        worldController!!.requestComponentState(entityId, componentIndex)
        console.log(rootValue, path, newValue)
    }

    fun demoClicked(){
        demoStep += 1
        val newVnode = demoRender()
        lastVnode = patch(lastVnode, newVnode)
    }

    fun demoRender() =
        when(demoStep) {
            0 ->
                h("div",
                    VNodeData(on = j("click" to ::demoClicked)),
                    "snabbdom-kotlin")

            1 ->
                h_("div#container.one.class", VNodeData(on = j("click" to ::demoClicked)), arrayOf(
                    h("span", VNodeData(style = j("fontWeight" to "bold")), "This is bold"),
                    " and this is just normal text",
                    h("a", VNodeData(props = j("href" to "/foo")), "I\"ll take you places!")
                ))

            else ->
                h_("div#container.two.Classes", arrayOf(
                    h("span", VNodeData(style = j("fontWeight" to "normal")), "This is normal"),
                    " and this is just normal text",
                    h("a", VNodeData(props = j("href" to "/foo")), "I\"ll take you places!")
                ))
        }

}

fun dataTypeToIcon(dt: DataType, isPrimitive: Boolean): RNode {
    val text = when (dt) {
        DataType.Boolean ->
            if (isPrimitive) "⅟" else "፧"
        DataType.Byte ->
            if (isPrimitive) "b" else "B"
        DataType.Short ->
            if (isPrimitive) "s" else "S"
        DataType.Int ->
            if (isPrimitive) "i" else "I"
        DataType.Long ->
            if (isPrimitive) "l" else "L"
        DataType.Float ->
            if (isPrimitive) "f" else "F"
        DataType.Double ->
            if (isPrimitive) "d" else "D"
        DataType.String ->
            "\uD83D\uDDB9"
        DataType.Enum ->
            "e"
        DataType.Object ->
            "O"
        else ->
            "※"
    }

    return column(attrs(width(px(28))), text("($text) "))
}