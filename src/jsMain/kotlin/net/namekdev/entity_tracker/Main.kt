package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener
import net.namekdev.entity_tracker.connectors.WorldUpdateMultiplexer
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.network.ExternalInterfaceCommunicator
import net.namekdev.entity_tracker.network.RawConnectionCommunicator
import net.namekdev.entity_tracker.network.RawConnectionOutputListener
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

abstract class RenderRoot : Invalidable {
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

    override fun invalidate() = notifyUpdate()

    abstract fun renderView()
}

class Main(container: HTMLElement) : RenderRoot(), IWorldUpdateListener<CommonBitVector> {
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
    val entities = ECSModel()
    var worldView = ValueContainer<WorldView?>(null).named("World.worldView")

    var connection: WebSocketClient? = null
    var connectionHostname = "localhost"
    var connectionPort = 8025
    fun connectionString() = "ws://$connectionHostname:$connectionPort/actions"
    var connectionStatus = ValueContainer<Boolean>(false).named("World.connectionStatus")

    init {
        // due to JS compilation - render() can't be called before fields are initialized, so delay it's first execution
        window.setTimeout({
            lastVnode = patch(container, h("div"))
        }, 0)

        fun update() {
//            lastVnode = patch(lastVnode, render())
//            window.setTimeout({
//                update()
//            }, 50)
        }
//        update()

        val artemisUiCommunicator = ExternalInterfaceCommunicator(worldUpdateListener)
        connection = WebSocketClient()
        connection?.let {
            it.listeners.add(artemisUiCommunicator)
            it.listeners.add(object: RawConnectionCommunicator {
                override fun connected(identifier: String, output: RawConnectionOutputListener) {
                    connectionStatus.value = true
                }

                override fun disconnected() {
                    connectionStatus.value = false

                    // auto-reconnect
                    window.setTimeout({
                        it.connect(connectionString())
                    }, 500)
                }
            })
            it.connect(connectionString())
        }

        invalidate()
    }

    val opts = OptionRecord(HoverSetting.AllowHover, FocusStyle())

    override fun renderView() {
        val rendering = RenderSession(this)
        val ctx: RNode = view(rendering)
        ctx.stylesheet?.let {
            dynamicStyles.innerHTML = toStyleSheetString(opts, it.values)
        }
        lastVnode = patch(lastVnode, ctx.vnode)

        console.log("update")
    }

    val view = renderTo(connectionStatus, worldView) { r, isConnected, worldView ->
        column(
            attrs(widthFill),
            (if (isConnected)
                row(
                    attrs(spacing(5)),
                    // TODO disconnect button; hostname, port info as read-only text
                    text("title: <TODO_title>")
                )
            else
                row(
                    attrs(spacing(5)),
                    text("hostname: "),
                    // TODO add inputs: hostname, port; and connect/disconnect button; and connection state
                    text("port: ")
                )),
            worldView?.render(r) ?: text("connecting...")
        )
    }.named("mainView")


    override fun worldDisconnected() {
        worldView()?.let {
            worldUpdateListener.listeners.remove(it)
        }
        worldController = null
        worldView.value = null
        entities.clear()
    }

    // it's called when connection is successfully established
    override fun injectWorldController(controller: IWorldController) {
        worldController = controller

        WorldView({ entities }, { worldController }).let {
            worldUpdateListener.listeners.add(it)
            worldView.value = it
        }
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

        entities.allSystems.update { it.add(index, systemInfo) }
    }

    override fun addedManager(name: String) {
        entities.allManagersNames.update { it.add(name) }
    }

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
        entities.setComponentType(index, info)
    }

    override fun updatedEntitySystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int) {
        entities.allSystems.update {
            val system = it[systemIndex]
            system.entitiesCount = entitiesCount
            system.maxEntitiesCount = maxEntitiesCount
        }
    }

    override fun addedEntity(entityId: Int, components: CommonBitVector) {
        entities.addEntity(entityId, components)
    }

    override fun deletedEntity(entityId: Int) {
        entities.removeEntity(entityId)
    }
}
