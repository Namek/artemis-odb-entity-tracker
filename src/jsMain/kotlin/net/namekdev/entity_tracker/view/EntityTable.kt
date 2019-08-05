package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.utils.*
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import snabbdom.Hooks
import snabbdom.VNodeData
import snabbdom.h
import snabbdom.j
import snabbdom.modules.Props

class EntityTable(
    val entities: () -> ECSModel,
    onComponentClicked: (entityId: Int, cmpIndex: Int) -> Unit
) {
    val render = renderTo(
        entities().entityComponents,
        entities().componentTypes,
        entities().highlightedComponentTypes,
        entities().entityFilterByComponentType
    ) { r, entityComponents, componentTypes, highlightedComponentTypes, entityFilterByComponentType ->
        val canvasHooks: Hooks = j()
        canvasHooks.insert = {
            console.log("create")

            val canvasEl = it.elm!! as HTMLCanvasElement
            val ctx = canvasEl.getContext("2d")!! as CanvasRenderingContext2D
            ctx.beginPath()
            ctx.lineWidth = 0.5
            ctx.moveTo(0.0, 0.0)
            ctx.lineTo(100.0, 100.0)
            ctx.stroke()
        }

        val containerProps: Props = j("width" to 400, "height" to "50vh")

        Unstyled(html = {
            h("div", VNodeData(key = "the-table", props = containerProps),
                h("canvas", VNodeData(hook = canvasHooks)))
        })
    }

    val render_ = renderTo(
        entities().entityComponents,
        entities().componentTypes,
        entities().highlightedComponentTypes,
        entities().entityFilterByComponentType
    ) { r, entityComponents, componentTypes, highlightedComponentTypes, entityFilterByComponentType ->
        console.asDynamic().time("EntityTable")
        val idCol = column(gridHeaderColumnStyle_common,
            row(gridHeaderColumnStyle_id, text("id")),
            el(attrs(height(px(underHeaderColumnsHeight))),
                textEdit("", InputType.Integer, false, width = 34,
                    onChange = { _, _ ->
                        // TODO apply the id filter
                    },
                    onEscape = {
                        // TODO clear the id filter
                    }))
        )

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

            column(gridHeaderColumnStyle_common,
                column(attrs(alignBottom, centerX),
                    row(columnStyle, text(it.name)),
                    el(attrs(centerX, height(px(underHeaderColumnsHeight)), paddingTop(4)),
                        filterOrIconForHighlightedAspectPartType))
            )
        }

        tmpAllRows.clear()
        val header = row(attrs(gridRowStyle), idCol, *componentCols)
        tmpAllRows.add(header)

        val allRows = viewEntitiesDataRows(r)

        val result = column(attrs(Attribute.StyleClass(Flag.height, Single("h-50vh", "height", "50vh"))),
            el("div", gridStyle(), allRows))

        console.asDynamic().timeEnd("EntityTable")

        result
    }.named("EntityTable.render")

    private val tmpAllRows = arrayListOf<RNode>()
    private val tmpComponentColsByEntityRow = arrayListOf(arrayListOf<RNode>())

    val viewEntitiesDataRows = renderTo(
        entities().componentTypes,
        entities().entityComponents,
        entities().entityFilterByComponentType
    ) { r, componentTypes, entityComponents, filters ->
        var tmpIndex = -1
        tmpComponentColsByEntityRow.ensureCapacity(entityComponents.size)

        entityComponents.filterMapTo(tmpAllRows) { (entityId, components) ->
            for ((filteredCmpIndex, filterType) in filters) {
                val hasComponent = components[filteredCmpIndex]

                if (filterType == ComponentTypeFilter.Include && !hasComponent ||
                    filterType == ComponentTypeFilter.Exclude && hasComponent)
                    return@filterMapTo null
            }


            tmpIndex += 1
            var tmpCols = tmpComponentColsByEntityRow.getOrNull(tmpIndex)
            if (tmpCols == null) {
                tmpCols = ArrayList(componentTypes.size+1)
                tmpComponentColsByEntityRow.add(tmpCols)
            }
            else tmpCols.clear()

            tmpCols.add(el(attrs(alignRight, paddingRight(idColRightPadding)), text(entityId.toString())))

            val entityComponentRow = componentTypes.indices.mapTo(tmpCols) { cmpIndex ->
                if (components[cmpIndex])
                    column(
                        attrs(
                            widthFill//, on(click = { onComponentClicked(entityId, cmpIndex) })
                        ),
                        el("div", attrs(centerX), text("x"))
                    )

                else text("")
            }

            row(entityId.toString(), attrs(gridRowStyle),
                entityComponentRow)
        }
    }.named("viewEntitiesDataRows")


    private val gridStyle = entities().componentTypes.cachedMap { componentTypes ->
        val columnCount = 1 + componentTypes.size

        attrs(
            Attribute.Class(0, tableGridClassName),
            padding(1, 10, 1, 3),
            style("display", "grid !important"),
            style("grid-template-columns", "repeat($columnCount, minmax(35px, auto))"),
            Attribute.StyleClass(Flag.height, Single("maxh-50vh", "max-height", "50vh")),
            style("overflow-y", "scroll")
        )
    }

    companion object {
        private val tableGridClassName = "grid-table"
        private val gridRowClassName = "gr-row"

        private val gridHeaderColumnStyle_common = attrs(
            // header should be not scrollable, rather on top of the table content
            backgroundColor(hexToColor(0xFFFFFF)),
            style("position", "sticky"),
            style("top", "-1px"), // fixes blurry effect of "sticky", Web Chrome engine
            style("z-index", "10")
        )

        private val idColRightPadding = 12
        private val underHeaderColumnsHeight = 24

        private val gridHeaderColumnStyle_id = attrs(
            alignBottom,
            alignRight,
            padding(0, idColRightPadding, 4, 0)
        )

        private val gridHeaderColumnStyle_component = attrs(
            centerX,

            // rotate text 90 degrees to squeeze all columns horizontally
            style("writing-mode", "vertical-lr"),
            style("transform", "rotate(-180deg)"),
            style("font-family", "sans-serif"),
            paddingTop(6)
        )

        private val gridRowStyle = Attribute.StyleClass(0, Single(gridRowClassName, "display", "contents !important"))

        // stylesheet that was hard to write using global stylesheet merging mechanism
        val additionalStyleSheet = """
            .${tableGridClassName} .${gridRowClassName}:not(:first-child):hover > * {
              background-color: #eee;
            }
            
            .${tableGridClassName} .${gridRowClassName}:not(:first-child) > div:hover  {
              background-color: #ccc !important;
            }
        """.trimIndent()
    }
}