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
        val idCol = column(gridHeaderColumnStyle, text("id"))
        val componentCols = componentTypes.mapToArray {
            row(gridHeaderColumnStyle, text(it.name))
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
                        text("x")
                    )

                else text("")
            }

            row(attrs(gridRowStyle),
                text(entityId.toString()), *entityComponents)
        }
    }.named("viewEntitiesDataRows")


    private val gridStyle = entities().componentTypes.cachedMap { componentTypes ->
        val columnCount = 1 + componentTypes.size
        attrs(
            Attribute.Class(0, tableGridClassName),
            padding(1),
            Attribute.StyleClass(0, Single("gr-display", "display", "grid !important")),
            Attribute.StyleClass(0, Single("gr-cols-$columnCount", "grid-template-columns", "repeat($columnCount, minmax(40px, auto))")),
            Attribute.StyleClass(Flag.height, Single("maxh", "max-height", "50vh")),
            Attribute.StyleClass(0, Single("ova", "overflow", "auto"))
        )
    }

    companion object {
        private val tableGridClassName = "grid-table"
        private val gridRowClassName = "gr-row"

        private val gridHeaderColumnStyle = attrs(
            backgroundColor(hexToColor(0xFFFFFF)),
            paddingRight(15),
            Attribute.StyleClass(0, Single("p-st", "position", "sticky")),
            Attribute.StyleClass(0, Single("pos-top", "top", "-1px")), // fixes blurry effect of "sticky", Web Chrome engine
            Attribute.StyleClass(0, Single("zidx", "z-index", "10"))
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