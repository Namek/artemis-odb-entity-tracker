package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.connectors.WorldController
import net.namekdev.entity_tracker.connectors.WorldUpdateInterfaceListener
import net.namekdev.entity_tracker.connectors.WorldUpdateListener.Companion.ENTITY_ADDED
import net.namekdev.entity_tracker.connectors.WorldUpdateListener.Companion.ENTITY_DELETED
import net.namekdev.entity_tracker.connectors.WorldUpdateListener.Companion.ENTITY_SYSTEM_STATS
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.network.ExternalInterfaceCommunicator
import net.namekdev.entity_tracker.network.WebSocketClient
import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.ui.Classes
import net.namekdev.entity_tracker.utils.CommonBitVector
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import snabbdom.modules.*
import snabbdom.*
import kotlin.browser.document
import kotlin.browser.window


fun main(args: Array<String>) {
    window.onload = {
        val style = document.createElement("style")
        style.asDynamic().type = "text/css"
        style.innerHTML = globalStylesheet
        document.getElementsByTagName("head")[0]!!.appendChild(style)

        val rootEl = document.createElement("div") as HTMLElement
        rootEl.classList.add(Classes.root)
        rootEl.classList.add(Classes.any)
        rootEl.classList.add(Classes.single)
        document.body!!.appendChild(rootEl)
        val container = document.createElement("div") as HTMLElement
        rootEl.appendChild(container)

        Main(container)
    }
}

class EntityTableModel {
    val entityComponents = mutableMapOf<Int, CommonBitVector>() // HashMap<Int, CommonBitVector>()
    val componentTypes = mutableListOf<ComponentTypeInfo>()


    fun setComponentType(index: Int, info: ComponentTypeInfo) {
        componentTypes.add(index, info)
    }

    fun addEntity(entityId: Int, components: CommonBitVector) {
        entityComponents.put(entityId, components)
    }

    fun removeEntity(entityId: Int) {
        entityComponents.remove(entityId)
    }

    fun getEntityComponents(entityId: Int): CommonBitVector =
        entityComponents[entityId]!!


    fun getComponentTypeInfo(index: Int): ComponentTypeInfo =
        componentTypes.get(index)

    fun clear() {
        componentTypes.clear()
        entityComponents.clear()

//        fireTableStructureChanged()
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
    var lastVnode: VNode

    var demoStep = 0
    val entities = EntityTableModel()

    var worldController: WorldController? = null
    var client: WebSocketClient? = null

    init {
//        var i = 0
//        for (col in arrayOf("Position", "Velocity", "Orientation"))
//            entities.setComponentType(i++, ComponentTypeInfo(col))
//
//        for (i in 1..10)
//            entities.addEntity(i, CommonBitVector(longArrayOf(7)))

        lastVnode = patch(container, view())

        fun update() {
            lastVnode = patch(lastVnode, view())
//            window.setTimeout({
//                update()
//            }, 50)
        }
//        update()

        client = WebSocketClient(ExternalInterfaceCommunicator(this))
        client!!.connect("ws://localhost:8025/actions")
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
        val hasAspect = allTypes != null || oneTypes != null || notTypes != null

//        baseSystemsTableModel!!.setSystem(index, name)

        if (hasAspect) {
//            entitySystemsTableModel!!.setSystem(index, name)
        }
        update()
    }

    override fun addedManager(name: String) {
        //  managersTableModel!!.addManager(name)
        update()
    }

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
        entities.setComponentType(index, info)
        update()
    }

    override fun updatedEntitySystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int) {
//        entitySystemsTableModel!!.updateSystem(systemIndex, entitiesCount, maxEntitiesCount)
        update()
    }

    override fun addedEntity(entityId: Int, components: CommonBitVector) {
        entities.addEntity(entityId, components)
        update()
    }

    override fun deletedEntity(entityId: Int) {
        entities.removeEntity(entityId)
        update()
    }


    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
        //         context.eventBus.updatedComponentState(entityId, componentIndex, valueTree)
        update()
    }

    fun update() {
        lastVnode = patch(lastVnode, view())
        console.log("update")
    }

    fun view() =
        column(arrayOf(width(fill)),
            viewEntitiesTable(),
            viewEntitiesFilters(),
            row(arrayOf(width(fill)),
                viewSystems(),
                viewCurrentEntity())
        )


    fun viewEntitiesTable(): VNode {
        val idCol = h("th", "entity id")
        val componentCols = entities.componentTypes.map { h("th", it.name) }.toTypedArray()
        val entitiesDataRows: Array<VNode> = entities.entityComponents.map { (entityId, components) ->
            val entityComponents = entities.componentTypes.indices.map { cmpIndex ->
                h("td", if (components[cmpIndex]) "x" else "")
            }.toTypedArray()

            h("tr", arrayOf(h("td", entityId.toString()), *entityComponents))
        }.toTypedArray()

        val headerAndRows = arrayOf(h("tr", arrayOf(idCol, *componentCols)), *entitiesDataRows)
        return el("table", attrs = arrayOf(width(fill)), nodes = headerAndRows)
    }

    fun viewEntitiesFilters() =
        row(arrayOf(h("span", "TODO filters here?")))

    fun viewSystems(): VNode =
        text("systems")

    fun viewCurrentEntity(): VNode =
        h("div", "current entity")


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

