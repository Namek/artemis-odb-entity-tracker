package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.utils.cachedMap
import net.namekdev.entity_tracker.utils.mapToArray
import net.namekdev.entity_tracker.utils.named
import net.namekdev.entity_tracker.utils.renderTo

class EntityTable(
    entities: () -> ECSModel,
    onComponentClicked: (entityId: Int, cmpIndex: Int) -> Unit
) {
    val render = renderTo(entities().entityComponents, entities().componentTypes) { r, entityComponents, componentTypes ->
        val idCol = row(gridHeaderColumnStyle_id, text("id"))
        val componentCols = componentTypes.mapToArray {
            row(gridHeaderColumnStyle_component, text(it.name))
        }
        val header = row(attrs(gridRowStyle), idCol, *componentCols)
        val entitiesDataRows = viewEntitiesDataRows(r)

        el("div", gridStyle(), header, *entitiesDataRows)
    }.named("EntityTable.render")

    val viewEntitiesDataRows = renderTo(entities().componentTypes, entities().entityComponents) { r, componentTypes, entityComponents ->
        entityComponents.mapToArray { (entityId, components) ->
            val entityComponents = componentTypes.indices.mapToArray { cmpIndex ->
                if (components[cmpIndex])
                    column(
                        attrs(
                            widthFill,
                            onClick { onComponentClicked(entityId, cmpIndex) }
                        ),
                        el("div", attrs(centerX), text("x"))
                    )

                else text("")
            }

            row(attrs(gridRowStyle),
//                el(attrs(fontAlignRight), text(entityId.toString())), // TODO should it work? why doesn it?
                el(attrs(alignRight, paddingRight(idColRightPadding)), text(entityId.toString())),
                *entityComponents)
        }
    }.named("viewEntitiesDataRows")


    private val gridStyle = entities().componentTypes.cachedMap { componentTypes ->
        val columnCount = 1 + componentTypes.size

        attrs(
            Attribute.Class(0, tableGridClassName),
            padding(1, 1, 1, 1),
            style("display", "grid !important"),
            style("grid-template-columns", "repeat($columnCount, minmax(35px, auto))"),
            Attribute.StyleClass(Flag.height, Single("maxh", "max-height", "50vh")),
            style("overflow", "auto")
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

        private val gridHeaderColumnStyle_id = gridHeaderColumnStyle_common + attrs(
            alignBottom,
            alignRight,
            padding(0, idColRightPadding, 4, 0)
        )

        private val gridHeaderColumnStyle_component = gridHeaderColumnStyle_common + attrs(
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