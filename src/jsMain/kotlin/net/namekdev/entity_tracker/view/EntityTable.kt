package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.utils.*
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import snabbdom.*
import snabbdom.modules.Props
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign

class Size(val width: Double, val height: Double)
fun getContainerSize(container: HTMLElement): Size {
    val compStyles = window.getComputedStyle(container);
    return Size(
        compStyles.width.substring(0, compStyles.width.length - 2).toDouble(),
        compStyles.height.substring(0, compStyles.height.length - 2).toDouble()
    )
}

class Scroll(
    var pos: Double = 0.0, // percent [0..1]
    var firstEntityIndexWithTranslation: Double = 0.0,
    var shouldRender: Boolean = false,
    var top: Double = 0.0,
    var left: Double = 0.0,
    var right: Double = 0.0,
    var bottom : Double = 0.0,
    var height: Double = 0.0,
    var containerTop: Double = 0.0,
    var containerLeft: Double = 0.0,
    var containerWidth: Double = 0.0,
    var containerHeight: Double = 0.0,
    var isDragged: Boolean = false,
    var dragOffsetY: Double = 0.0,
    var isFocused: Boolean = false
)
class Hover(
    var lastMousePosX: Double = 0.0,
    var lastMousePosY: Double = 0.0,
    var justClicked: Boolean = false
)

var entityTableId: Int = -1

class EntityTable(
    val entities: () -> ECSModel,
    onComponentClicked: (entityId: Int, cmpIndex: Int) -> Unit
) {
    val PIXEL_RATIO: Double
    val r: Double

    lateinit var canvas: HTMLCanvasElement
    lateinit var ctx: CanvasRenderingContext2D
    var canvasWidth: Double = 0.0
    var canvasHeight: Double = 0.0
    var needsRedraw = false

    val scroll = Scroll()
    val hover = Hover()

    init {
        entityTableId += 1

        val ctx = (document.createElement("canvas") as HTMLCanvasElement)
            .getContext("2d").asDynamic()

        val dpr = window.devicePixelRatio ?: 1.0
        val bsr: Double = (ctx.webkitBackingStorePixelRatio ?:
              ctx.mozBackingStorePixelRatio ?:
              ctx.msBackingStorePixelRatio ?:
              ctx.oBackingStorePixelRatio ?:
              ctx.backingStorePixelRatio ?: 1.0)

        PIXEL_RATIO = dpr / bsr
        r = 1/PIXEL_RATIO
    }

    private val entityCount: Int
        get() = entities().entityComponents.value.size

    fun refreshCanvasSize(canvas: HTMLCanvasElement) {
        val size = getContainerSize(canvas.parentNode as HTMLElement)

        canvas.width = floor(size.width * PIXEL_RATIO).roundToInt()
        canvas.height = floor(size.height * PIXEL_RATIO).roundToInt()
        canvas.style.width = (canvas.width / PIXEL_RATIO).toString() + "px"
        canvas.style.height = (canvas.height / PIXEL_RATIO).toString() + "px"
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.setTransform(PIXEL_RATIO, 0.0, 0.0, PIXEL_RATIO, 0.0, 0.0)
        canvasWidth = canvas.width*r
        canvasHeight = canvas.height*r
    }

    private val createCanvas = ThunkFn.args1 {
        val hooks: Hooks = j()

        hooks.insert = {
            canvas = (it.elm!! as HTMLElement).firstChild as HTMLCanvasElement
            ctx = canvas.getContext("2d")!! as CanvasRenderingContext2D

            window.addEventListener("resize", ::onWindowResize)
            canvas.addEventListener("mousedown", ::onCanvasMouseDown)
            window.addEventListener("mouseup", ::onWindowMouseUp)
            canvas.addEventListener("mousemove", ::onCanvasMouseMove)
            window.addEventListener("mousemove", ::onWindowMouseMove)
            canvas.addEventListener("mouseleave", ::onCanvasMouseLeave)
            canvas.addEventListener("click", ::onCanvasClick)
            canvas.addEventListener("wheel", ::onCanvasWheel)

            // The timeout could be 0 for Firefox but the Chrome browser relayouts elements in some other way.
            // With Chrome 0 timeout would result in element being too big (height: 300 vs 251 px).
            window.setTimeout({ refreshCanvasSize(canvas)}, 100)

            entities().entityComponents.updateListeners.add(::requestRedrawCanvas)
            entities().componentTypes.updateListeners.add(::requestRedrawCanvas)
            entities().highlightedComponentTypes.updateListeners.add(::requestRedrawCanvas)
            entities().entityFilterByComponentType.updateListeners.add(::requestRedrawCanvas)
        }
        hooks.destroy = {
            window.removeEventListener("resize", ::onWindowResize)
            canvas.removeEventListener("mousedown", ::onCanvasMouseDown)
            window.removeEventListener("mouseup", ::onWindowMouseUp)
            canvas.removeEventListener("mousemove", ::onCanvasMouseMove)
            window.removeEventListener("mousemove", ::onWindowMouseMove)
            canvas.removeEventListener("mouseleave", ::onCanvasMouseLeave)
            canvas.removeEventListener("click", ::onCanvasClick)
            canvas.removeEventListener("wheel", ::onCanvasWheel)

            entities().entityComponents.updateListeners.remove(::requestRedrawCanvas)
            entities().componentTypes.updateListeners.remove(::requestRedrawCanvas)
            entities().highlightedComponentTypes.updateListeners.remove(::requestRedrawCanvas)
            entities().entityFilterByComponentType.updateListeners.remove(::requestRedrawCanvas)
        }

        val containerProps: Props = j("width" to "auto")

        h("div", VNodeData(props = containerProps, hook = hooks),
            h("canvas"))
    }

    private fun onWindowResize(evt: Event) {
        refreshCanvasSize(canvas)
        requestRedrawCanvas()
    }

    private fun onCanvasMouseDown(e: Event) {
        val x = (e as MouseEvent).offsetX
        val y = e.offsetY

        if (scroll.shouldRender) {
            if (x > scroll.left && x < scroll.right && y > scroll.top && y < scroll.bottom) {
                scroll.dragOffsetY = y - scroll.top
                scroll.isDragged = true
                requestRedrawCanvas()
            }
        }
    }
    private fun onCanvasMouseMove(e: Event) {
        val x = (e as MouseEvent).offsetX
        val y = e.offsetY

        if (x > scroll.left && x < scroll.right && y > scroll.top && y < scroll.bottom) {
            scroll.isFocused = true
            requestRedrawCanvas()
        }

        if (hover.lastMousePosX != x || hover.lastMousePosY != y) {
            hover.lastMousePosX = x
            hover.lastMousePosY = y
            requestRedrawCanvas()
        }
    }

    private fun onCanvasMouseLeave(e: Event) {
        hover.lastMousePosX = -1.0
        hover.lastMousePosY = -1.0
        requestRedrawCanvas()
    }

    private fun onCanvasClick(e: Event) {
        val x = (e as MouseEvent).offsetX
        val y = e.offsetY

        val scrollAreaButNotScrollEl = x > scroll.left && x < scroll.right && !(y > scroll.top && y < scroll.bottom)
        if (scrollAreaButNotScrollEl) {
            scroll.dragOffsetY = scroll.height / 2

            setScrollPos(
                (y - scroll.containerTop - scroll.dragOffsetY) / (scroll.containerHeight - scroll.height)
            )
        }

        // detect clicking on entity component
        hover.justClicked = true

        requestRedrawCanvas()
    }

    private fun onWindowMouseUp(e: Event) {
        scroll.isDragged = false
        requestRedrawCanvas()
    }

    private fun onWindowMouseMove(e: Event) {
        if (scroll.isDragged) {
            e.preventDefault()
            val localY = (e as MouseEvent).clientY - canvas.getBoundingClientRect().top

            setScrollPos(
                (localY - scroll.containerTop - scroll.dragOffsetY) / (scroll.containerHeight - scroll.height)
            )
        }
    }

    private fun onCanvasWheel(e: Event) {
        val diff: Double = sign((e as WheelEvent).deltaY)* 200.0 / (entityCount * rowHeight)
        setScrollPos(scroll.pos + diff)
    }

    val render = renderTo(
        entities().entityComponents,
        entities().componentTypes,
        entities().highlightedComponentTypes,
        entities().entityFilterByComponentType
    ) { r, entityComponents, componentTypes, highlightedComponentTypes, entityFilterByComponentType ->
        val idCol = column(gridHeaderColumnStyle_id,
            text("id"),
            el(attrs(
                height(px(underHeaderColumnsHeight)),
                paddingRight((colsGap/2).toInt())),
                textEdit("", InputType.Integer, false, width = idInputWidth,
                    onChange = { _, _ ->
                        // TODO apply the id filter
                    },
                    onEscape = {
                        // TODO clear the id filter
                    })))

        val componentCols = componentTypes.mapToArray {
            val cmpTypeIndex = it.index
            val highlightedAs = highlightedComponentTypes[cmpTypeIndex]
            val filter = entityFilterByComponentType[cmpTypeIndex]

            val filterOrIconForHighlightedAspectPartType =
                when (highlightedAs) {
                    null -> {
                        val label = when (filter) {
                            null -> el(attrs(fontColor(hexToColor(0xeeeeee))), text("_"))
                            ComponentTypeFilter.Include -> text("+")
                            ComponentTypeFilter.Exclude -> text("-")
                        }
                        button(label) {
                            entities().toggleComponentTypeFilter(cmpTypeIndex)
                        }
                    }
                    AspectPartType.All -> text("A")
                    AspectPartType.One -> text("1")
                    AspectPartType.Exclude -> text("!")
                }

            var columnStyle = gridHeaderColumnStyle_component
            if (highlightedAs != null)
                columnStyle += attrs(backgroundColor(hexToColor(0xdddddd)))

            column(attrs(
                alignBottom, centerX,
                width(px((colsGap + crossSize).toInt())),
                padding(0, (colsGap/2).toInt(), 0, (colsGap/2).toInt())),
                row(columnStyle, text(it.name)),
                el(attrs(centerX, height(px(underHeaderColumnsHeight)), paddingTop(4)),
                    filterOrIconForHighlightedAspectPartType))
        }

        val header = row(idCol, *componentCols)

        column(attrs(Attribute.StyleClass(Flag.height, Single("h-50vh", "height", "50vh"))),
            header,
            Unstyled(html = {
                thunk("div.${canvasContainerName}", "entity-table-container", createCanvas, arrayOf(entityTableId))
            }))
    }

    private fun requestRedrawCanvas() {
        if (!needsRedraw) {
            redrawCanvas.invalidate()
            needsRedraw = true
            window.requestAnimationFrame { redrawCanvas() }
        }
    }

    private fun setScrollPos(pos: Double) {
        scroll.pos = kotlin.math.min(1.0, kotlin.math.max(0.0, pos))
        val visibleRowsCountFractioned = canvasHeight / rowHeight
        scroll.firstEntityIndexWithTranslation = (entityCount - visibleRowsCountFractioned) * scroll.pos

        requestRedrawCanvas()
    }

    val redrawCanvas = cachedMap(
        entities().entityComponents,
        entities().componentTypes,
        entities().highlightedComponentTypes,
        entities().entityFilterByComponentType
    ) { entityComponents, componentTypes, highlightedComponentTypes, entityFilterByComponentType ->
        ctx.clearRect(0.0, 0.0, canvasWidth, canvasHeight)

        val componentTypesCount = componentTypes.size
        val entityCount: Int = entityComponents.size
        val visibleRowsCountFractioned = canvasHeight / rowHeight
        val visibleRowsCount = ceil(visibleRowsCountFractioned).toInt()
        val maxScrollHeight = canvasHeight
        scroll.shouldRender = visibleRowsCountFractioned < entityCount
        scroll.containerHeight = maxScrollHeight
        scroll.containerWidth = scrollWidth.toDouble()
        scroll.containerLeft = 0.0
        scroll.containerTop = 0.0
        scroll.height = minScrollHeight + (maxScrollHeight - minScrollHeight) * (visibleRowsCount / entityCount.toDouble())
        scroll.top = scroll.containerTop + (maxScrollHeight - scroll.height) * scroll.pos
        scroll.left = scroll.containerLeft + scrollContainerPadding
        scroll.right = scroll.left + scrollWidth - scrollContainerPadding*2
        scroll.bottom = scroll.top + scroll.height
        
        
        ctx.strokeStyle = "#ddd"
        //ctx.rect(0, 0, canvasWidth, canvasHeight)
        
        // draw scroll on the left!
        if (scroll.shouldRender) {
            ctx.lineWidth = r
            ctx.rect(scroll.containerLeft, scroll.containerTop, scroll.containerWidth, scroll.containerHeight)
            ctx.stroke()
            ctx.fillStyle = "#ccc"
            ctx.fillRect(scroll.left, scroll.top + scrollContainerPadding, scrollWidth - scrollContainerPadding*2, scroll.height - scrollContainerPadding*2)
            
            //ctx.fillStyle = "red"
            //ctx.fillRect(scroll.left, scroll.top + scroll.dragOffsetY, scrollWidth - scrollContainerPadding*2, 3)
        }
    
        // draw rows representing game entities!
        val fontSize = 13
        ctx.font = "${fontSize}px monospace"
        ctx.strokeStyle = "black"
        ctx.fillStyle = "black"
        ctx.lineWidth = r
        
        val firstEntityIndexWithTranslation: Double =
            if (scroll.isDragged) {
                (entityCount - visibleRowsCountFractioned) * scroll.pos
            }
            else {
                scroll.firstEntityIndexWithTranslation
            }
        
        val firstEntityIndex = floor(firstEntityIndexWithTranslation).toInt()
        val startX = scrollWidth + colsGap
        val idColWidth = idInputWidth
        val rowWidth: Double = idColWidth + componentTypesCount * (crossSize+colsGap)
        
        var y: Double = -(firstEntityIndexWithTranslation % 1) * rowHeight

        // background for hovered column
        var hoveringCol = -1
        if (hover.lastMousePosX > 0) {
            var x = startX + idColWidth
            var colIndex = 0
            while (x < startX + rowWidth) {
                if (hover.lastMousePosX >= x - hoverMargin && hover.lastMousePosX < x + crossSize + hoverMargin) {
                    val oldFillStyle = ctx.fillStyle
                    ctx.fillStyle = "#f9f9f9"
                    ctx.fillRect(x - hoverMargin, 0.0, crossSize + 2 * hoverMargin, canvasHeight)
                    ctx.fillStyle = oldFillStyle
                    hoveringCol = colIndex
                    break
                }

                x += crossSize + colsGap
                colIndex += 1
            }
        }

        val eiEnd = kotlin.math.min(visibleRowsCount, entityCount - firstEntityIndex)
        for (ei in 0 until eiEnd) {
            // TODO id is not always just an id, we should iterate over existing IDs of the `entityComponents` collection
            val entityId = firstEntityIndex + ei
            val entity = entityComponents[entityId] ?: continue
            var x = startX
    
            // background for hovered row
            val isRenderingRowHover = hover.lastMousePosY >= y
                && hover.lastMousePosY < y + rowHeight
                && hover.lastMousePosX > startX
            if (isRenderingRowHover) {
                val oldFillStyle = ctx.fillStyle
                ctx.fillStyle = "#f3f3f3"
                ctx.fillRect(x, y, rowWidth, rowHeight)
                ctx.fillStyle = oldFillStyle
            }
    
            // entity id
            ctx.textAlign = CanvasTextAlign.RIGHT
            ctx.fillText(entityId.toString(), x + idColWidth, y + rowHeight - rowYPadding)
            x += idColWidth + colsGap // TODO fix `colsgap` for onmousemove
            y += rowYPadding
            
            // component set
            for (cmpIndex in 0 until componentTypesCount) {
                if (entity[cmpIndex]) {
                    if (isRenderingRowHover && hoveringCol == cmpIndex) {
                        val oldFillStyle = ctx.fillStyle
                        val size = crossSize + hoverMargin*2
                        ctx.fillStyle = "#ccc"
                        
                        if (hover.justClicked) {
                            ctx.fillStyle = "steelblue"
                            onComponentClicked(firstEntityIndex+ei, cmpIndex)

                            window.setTimeout({ requestRedrawCanvas() }, 100)
                        }
                        
                        ctx.fillRect(x - hoverMargin, y - hoverMargin, size, size)
                        ctx.fillStyle = oldFillStyle
                    }
                
                    ctx.beginPath()
                    ctx.moveTo(x, y)
                    ctx.lineTo(x + crossSize, y + crossSize)
                    ctx.moveTo(x + crossSize, y)
                    ctx.lineTo(x, y + crossSize)
                    ctx.stroke()
                }

                x += crossSize + colsGap
            }
            
            y += rowHeight - rowYPadding
        }

        hover.justClicked = false
        needsRedraw = false
    }


    companion object {
        // canvas properties
        val idInputWidth = 50.0
        val colsGap = 10.0
        val minScrollHeight = 20.0
        val scrollWidth = 15.0
        val crossSize = 8.0
        val rowYPadding = 4.0
        val rowHeight = crossSize + 2*rowYPadding
        val scrollContainerPadding = 2.0
        val hoverMargin = 3.0


        private val underHeaderColumnsHeight = 24

        private val gridHeaderColumnStyle_id = attrs(
            alignBottom,
            alignRight,
            padding(0, 0, 4, (scrollWidth + colsGap).toInt())
        )

        private val gridHeaderColumnStyle_component = attrs(
            centerX,
            width(px(crossSize.toInt())),

            // rotate text 90 degrees to squeeze all columns horizontally
            style("writing-mode", "vertical-lr"),
            style("transform", "rotate(-180deg)"),
            style("font-family", "sans-serif")
        )

        private val canvasContainerName = "entity-table-container"

        // stylesheet that was hard to write using global stylesheet merging mechanism
        val additionalStyleSheet = """
            .${canvasContainerName} {
              flex-grow: 1;
            }
        """.trimIndent()
    }
}