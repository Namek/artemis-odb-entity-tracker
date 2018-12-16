package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.ui.column
import net.namekdev.entity_tracker.ui.row
import net.namekdev.entity_tracker.utils.CommonBitVector
import org.w3c.dom.HTMLElement
import snabbdom.modules.*
import snabbdom.*
import kotlin.browser.document
import kotlin.browser.window


fun main(args: Array<String>) {
    window.onload = {
        Main(document.body!!)
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

class Main(container: HTMLElement) {
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



    init {
        var i = 0
        for (col in arrayOf("Position", "Velocity", "Orientation"))
            entities.setComponentType(i++, ComponentTypeInfo(col))

        for (i in 1..10)
            entities.addEntity(i, CommonBitVector(longArrayOf(7)))

        lastVnode = patch(container, view())

        fun update() {
            lastVnode = patch(lastVnode, view())
//            window.setTimeout({
//                update()
//            }, 50)
        }
        update()
    }

    fun view() =
        column(
            viewEntitiesTable(),
            viewEntitiesFilters(),
            row(viewSystems(), viewCurrentEntity())
        )


    fun viewEntitiesTable(): VNode {
        val idCol = h("th", "entity id")

        val componentCols = entities.componentTypes.map { h("th", it.name) }.toTypedArray()

        val entitiesData: Array<VNode> = entities.entityComponents.map { (entityId, components) ->
            val entityComponents = entities.componentTypes.indices.map { cmpIndex ->
                h("td", if (components[cmpIndex]) "x" else "")
            }.toTypedArray()

            h("tr", arrayOf(h("td", entityId.toString()), *entityComponents))
        }.toTypedArray()


        return h("table.entities",
            arrayOf(h("tr", arrayOf(idCol, *componentCols)), *entitiesData)
        )
    }

    fun viewEntitiesFilters() =
        row(arrayOf(h("span", "TODO filters here?")))

    fun viewSystems(): VNode {
        TODO()
    }

    fun viewCurrentEntity(): VNode {
        TODO()
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
                h_("div#container.two.classes", arrayOf(
                    h("span", VNodeData(style = j("fontWeight" to "normal")), "This is normal"),
                    " and this is just normal text",
                    h("a", VNodeData(props = j("href" to "/foo")), "I\"ll take you places!")
                ))
        }

}

