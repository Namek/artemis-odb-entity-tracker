package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.connectors.WorldController
import net.namekdev.entity_tracker.connectors.WorldUpdateInterfaceListener
import net.namekdev.entity_tracker.connectors.WorldUpdateListener.Companion.ENTITY_ADDED
import net.namekdev.entity_tracker.connectors.WorldUpdateListener.Companion.ENTITY_DELETED
import net.namekdev.entity_tracker.connectors.WorldUpdateListener.Companion.ENTITY_SYSTEM_STATS
import net.namekdev.entity_tracker.model.AspectInfo_Common
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.SystemInfo_Common
import net.namekdev.entity_tracker.network.ExternalInterfaceCommunicator
import net.namekdev.entity_tracker.network.WebSocketClient
import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.ui.Classes
import net.namekdev.entity_tracker.utils.*
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

class Main(container: HTMLElement) : WorldUpdateInterfaceListener<CommonBitVector> {
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

    var worldController: WorldController? = null
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

    override fun injectWorldController(controller: WorldController) {
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
        column(attrs(widthFill, heightFill, paddingXY(10, 10)),
            viewEntitiesTable(),
            viewEntitiesFilters(),
            row(attrs(widthFill),
                viewSystems(),
                viewCurrentEntity())
        )

    val viewEntitiesTable = transformMultiple(entities.entityComponents, entities.componentTypes) { entityComponents, componentTypes ->
        val idCol = thCell("entity id")
        val componentCols = componentTypes.mapToArray { thCell(it.name) }
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

        table(attrs(width(fill)), header, *entitiesDataRows)
    }

    fun showComponent(entityId: Int, componentIndex: Int) {
        entities.observedEntity.value = entityId
        notifyUpdate()
        worldController?.let {
            it.requestComponentState(entityId, componentIndex)
        }
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
        return table(attrs(width(fill)), header, *rows)
    }

    fun viewCurrentEntity(): RNode =
        row(attrs(widthFill, heightFill, alignTop),
            arrayOf(
                // TODO fixme: height fill does not work! "contentTop" given by column() may be ignored?
                column(attrs(widthFill, alignTop),
                    arrayOf(viewObservedEntity())
                ),
                column(attrs(widthFill, alignTop),
                    arrayOf(viewSelectedComponent())
                )
            )
        )

    val viewObservedEntity = transformMultiple(entities.observedEntity, entities.currentComponent) { entityId, currentComponent ->
        if (entityId == null)
            column(arrayOf(text("select entity")))
        else {
            val componentTypes = entities.entityComponents.value[entityId]
            if (componentTypes == null)
                column(arrayOf(text("error: component types for entity #$entityId were not found")))
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
                            text(cmpType.name))
                    )

                    i = componentTypes.nextSetBit(i+1)
                }

                column(arrayOf(),
                    text("Entity #$entityId"),
                    text("Components:"),
                    *componentNames.toTypedArray()
                )
            }
        }
    }

    val viewSelectedComponent = entities.currentComponent.transform { cmp ->
        if (cmp == null)
            column(arrayOf(text("")))
        else {
            viewValueTree(cmp.valueTree.model!!, cmp.valueTree)
        }
    }

    fun viewValueTree(model: ObjectModelNode, value: Any?, level: Int = 0): RNode {
//        console.log(model, value)
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
                val enumTypeName = enumDescription.name
                val enumValueName = enumDescription.children!![model.enumValue].name
                text("${model.name ?: ""}<$enumTypeName> = " + enumValueName)
            }
            else
                text("${model.name ?: ""}<${model.dataType.name}> = " + value)
        }
        else {
            val vt = value as ValueTree
            val fields = model.children!!
                .mapIndexed { i, fieldModel ->
                    val fieldValue = vt.values[i]
                    viewValueTree(fieldModel, fieldValue, level + 1)
                }

            if (level > 0)
                column(attrs(),
                    text("${model.name ?: ""}<${model.dataType.name}>:"),
                    column(attrs(paddingLeft(12)), fields.toTypedArray())
                )
            else
                column(fields.toTypedArray())
        }
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
