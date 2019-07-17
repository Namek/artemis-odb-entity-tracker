package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener
import net.namekdev.entity_tracker.connectors.WorldUpdateMultiplexer
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.network.ExternalInterfaceCommunicator
import net.namekdev.entity_tracker.network.WebSocketClient
import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.ui.Classes
import net.namekdev.entity_tracker.utils.*
import net.namekdev.entity_tracker.view.AspectInfo
import net.namekdev.entity_tracker.view.ECSModel
import net.namekdev.entity_tracker.view.SystemInfo
import net.namekdev.entity_tracker.view.WorldView
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


class Main(container: HTMLElement) : IWorldUpdateListener<CommonBitVector> {
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
    private var dynamicStyles: Element = createStyleElement("")

    private val worldUpdateListener = WorldUpdateMultiplexer<CommonBitVector>(mutableListOf(this))
    var worldController: IWorldController? = null
    var connection: WebSocketClient? = null
    val entities = ECSModel()
    var worldView: WorldView? = null


    init {
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

        connection = WebSocketClient(ExternalInterfaceCommunicator(worldUpdateListener))
        connection!!.connect("ws://localhost:8025/actions")
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

    val opts = OptionRecord(HoverSetting.AllowHover, FocusStyle())

    fun renderView() {
        val ctx: RNode = view()
        ctx.stylesheet?.let {
            dynamicStyles.innerHTML = toStyleSheetString(opts, it.values)
        }
        lastVnode = patch(lastVnode, ctx.vnode)

        console.log("update")
    }

    fun view() =
        column(attrs(widthFill),
            (if (connection?.isConnected == true)
                row(attrs(spacing(5)),
                    // TODO disconnect button; hostname, port info as read-only text
                    text("title: <TODO_title>"))
            else
                row(attrs(spacing(5)),
                    text("hostname: "),
                    // TODO add inputs: hostname, port; and connect/disconnect button; and connection state
                    text("port: "))),
            worldView?.view() ?: text("connecting..."))


    override fun worldDisconnected() {
        worldView?.let {
            worldUpdateListener.listeners.remove(it)
            it.dispose()
        }
        worldView = null
        entities.clear()

        // TODO initiate auto-reconnect somehow (maybe use MemoTransformers?)
        connection = null
    }

    // it's called when connection is successfully established
    override fun injectWorldController(controller: IWorldController) {
        worldController = controller

        worldView?.dispose()
        worldView = WorldView(notifyUpdate, { entities }, { worldController })
        worldView?.let {
            worldUpdateListener.listeners.add(it)
        }
        notifyUpdate()
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
}
