package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateInterfaceListener
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


    val observedEntityId = MemoContainer<Int?>(null)
    val currentComponent = MemoContainer<CurrentComponent?>(null)
    var currentComponentIsWatched = false
    var currentlyEditedInput: EditedInputState? = null


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
        currentComponent.value?.let {
            // if current component changes, do not allow
            // re-showing the <input> when we get to previous component (it just looks weird)
            if (it.entityId != entityId || it.componentIndex != componentIndex)
                currentlyEditedInput = null
        }

        currentComponent.value?.let {
            if (it.entityId != entityId || it.componentIndex != componentIndex)
                worldController!!.setComponentStateWatcher(it.entityId, it.componentIndex, false)
        }

        // if that's first component ever chosen then watch it automatically
        if (currentComponent.value == null) {
            worldController!!.setComponentStateWatcher(entityId, componentIndex, true)
            currentComponentIsWatched = true
        }

        currentComponent.value = CurrentComponent(entityId, componentIndex, valueTree as ValueTree)
        notifyUpdate()
    }

    val notifyUpdate = {
        // a very simple debouncer using the fact that JavaScript is single-threaded
        var alreadyRequested = false

        {
            if (!alreadyRequested) {
                alreadyRequested = true

                // timeout is because of JS compilation - we have lateinit vars!
                window.setTimeout({
                    alreadyRequested = false
                    renderView()
                }, 0)
            }
        }
    }()

    fun notifyCurrentlyEditedInputChanged() {
        viewSelectedComponent.cachedResult = null
        viewCurrentEntity.cachedResult = null
        notifyUpdate()
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
                                onClick { showComponent(entityId, cmpIndex) }
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
        observedEntityId.value = entityId
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

    val viewCurrentEntity = transformMultiple(observedEntityId, currentComponent) { entityId, currentComponent ->
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
                    row(attrs(spacing(4)),
                        if (currentComponent == null)
                            dummyEl
                        else {
                            val componentIndex = currentComponent.componentIndex

                            row(attrs(spacing(4)),
                                checkbox(currentComponentIsWatched) { enabled ->
                                    worldController!!.setComponentStateWatcher(entityId, componentIndex, enabled)
                                    currentComponentIsWatched = enabled
                                },
                                text("Watch E$entityId:C$componentIndex")
                            )
                        }
                    ),
                    row(attrs(borderBottom(1)),
                        text("Entity #$entityId")
                    ),
                    viewObservedEntity()),
                column(attrs(widthFill, alignTop, spacing(6)),
                    row(attrs(borderBottom(0)),
                        elems(text(componentName?.let {"<$it>:"} ?: "" ))),
                    column(attrs(paddingLeft(12)), viewSelectedComponent()))
            )
        }
    }

    val viewObservedEntity = transformMultiple(observedEntityId, currentComponent) { entityId, currentComponent ->
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
                            onClick{ showComponent(entityId, cmpType.index) }
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

    val viewSelectedComponent = currentComponent.transform { cmp ->
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

                row(attrs(height(px(22))),
                    dataTypeToIcon(DataType.Enum, false),
                    text("${model.name ?: ""}<$enumTypeName> = "),
                    dropdown(value as Int?, enumValuesNames, true) {
                        onValueChanged(rootValue, path, model.dataType, it)
                    }
                )
            }
            else if (model.dataType == DataType.Boolean) {
                row(attrs(height(px(22))),
                    dataTypeToIcon(DataType.Boolean, model.isTypePrimitive),
                    text("${model.name ?: ""} = ⅟"),
                    nullableCheckbox(value as Boolean?, !model.isTypePrimitive) {
                        onValueChanged(rootValue, path, model.dataType, it)
                    }
                )
            }
            else {
                val inputType = convertDataTypeToInputType(model.dataType)
                val isNullable = !model.isTypePrimitive

                fun showEditor() =
                    row(attrs(spacing(4)),
                        text("${value?.toString() ?: "<null>"} →"),
                        textEdit(currentlyEditedInput?.text ?: "", inputType, true,
                            onChange = { _, str ->
                                if (currentlyEditedInput?.path == path) {
                                    currentlyEditedInput!!.text = str
                                }
                            },
                            onEnter = {
                                val newValue = extractInputValue(it, model.dataType)
                                onValueChanged(rootValue, path, model.dataType, newValue)
                            },
                            onEscape = {
                                currentlyEditedInput = null
                                notifyCurrentlyEditedInputChanged()
                            }
                        )
                    )

                fun showValueOrEditor() =
                    if (currentlyEditedInput?.path != path) {
                        row(attrs(
                            onClick {
                                currentlyEditedInput = EditedInputState(path, value?.toString() ?: "")
                                notifyCurrentlyEditedInputChanged()
                            }),
                            text(value?.toString() ?: "<null>")
                        )
                    }
                    else showEditor()

                row(attrs(height(px(22))),
                    dataTypeToIcon(model.dataType, model.isTypePrimitive),
                    text("${model.name ?: ""} = "),

                    if (!isNullable) {
                        showValueOrEditor()
                    }
                    else {
                        nullCheckbox(value == null,
                            onChange = { isNull ->
                                currentlyEditedInput =
                                    if (isNull) null
                                    else EditedInputState(path, value?.toString() ?: "")

                                val newValue =
                                    if (isNull) null
                                    else if (model.dataType == DataType.String) ""
                                    else 0

                                onValueChanged(rootValue, path, model.dataType, newValue)

                                notifyCurrentlyEditedInputChanged()
                            },
                            view = { isNull ->
                                if (!isNull)
                                    showValueOrEditor()
                                else
                                    text("<null>")
                            }
                        )
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
                column(attrs(spacing(4)),
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

fun extractInputValue(value: InputValue?, dataType: DataType): Any? =
    when (value) {
        null -> null
        is InputValueText -> value.text
        is InputValueFloatingPoint -> {
            when (dataType) {
                DataType.Float -> value.number.toFloat()
                DataType.Double -> value.number.toDouble()
                else ->
                    throw RuntimeException("textEdit() floating point should be only Float/Double and it is: ${dataType}")
            }
        }
        is InputValueInteger -> {
            when (dataType) {
                DataType.Byte -> value.number.toByte()
                DataType.Short -> value.number.toShort()
                DataType.Int -> value.number.toInt()
                DataType.Long -> value.number.toLong()
                else ->
                    throw RuntimeException("integer was supposed to be Byte/Short/Int/Long and it is: ${dataType}")
            }
        }
    }

fun convertDataTypeToInputType(dataType: DataType): InputType =
    when (dataType) {
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

data class EditedInputState(val path: List<Int>, var text: String?)