package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener
import net.namekdev.entity_tracker.ui.*
import net.namekdev.entity_tracker.utils.*
import net.namekdev.entity_tracker.utils.serialization.DataType
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ValueTree


class WorldView(
    val entities: () -> ECSModel,
    val worldController: () -> IWorldController?
) : IWorldUpdateListener<CommonBitVector> {
    val observedEntityId = ListenableValueContainer<Int?>(null).named("observedEntityId")
    val currentComponent = ListenableValueContainer<CurrentComponent?>(null).named("currentComponent")
    var currentComponentIsWatched = false
    var currentlyEditedInput: EditedInputState? = null


    override fun deletedEntity(entityId: Int) {
        if (observedEntityId.value == entityId) {
            observedEntityId.value = null
        }
        if (currentComponent.value?.entityId == entityId) {
            currentComponent.value = null
        }
//        notifyChanged()
    }

    /**
     * Received after component value request was sent.
     */
    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
        currentComponent()?.let {
            // if current component changes, do not allow
            // re-showing the <input> when we get to previous component (it just looks weird)
            if (it.entityId != entityId || it.componentIndex != componentIndex)
                currentlyEditedInput = null
        }

        currentComponent()?.let {
            if (it.entityId != entityId || it.componentIndex != componentIndex)
                worldController()!!.setComponentStateWatcher(it.entityId, it.componentIndex, false)
        }

        // if that's first component ever chosen then watch it automatically.
        if (currentComponent() == null) {
            worldController()!!.setComponentStateWatcher(entityId, componentIndex, true)
            currentComponentIsWatched = true
        }

        currentComponent.value = CurrentComponent(entityId, componentIndex, valueTree as ValueTree)
//        notifyChanged()
    }

    fun render(r: RenderSession) =
        column(
            attrs(widthFill, heightFill, paddingXY(10, 10), spacing(10)),
            viewEntitiesTable(r),
            viewEntitiesFilters(r),
            row(
                attrs(widthFill),
                viewCurrentEntity(r),
                viewSystems())
        )

    private fun notifyCurrentlyEditedInputChanged() {
        viewSelectedComponent.invalidate()
        viewCurrentEntity.invalidate()
//        notifyChanged()
    }

    val viewEntitiesTable = renderTo(entities().entityComponents, entities().componentTypes) { r, entityComponents, componentTypes ->
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
    }.named("viewEntitiesTable")

    fun showComponent(entityId: Int, componentIndex: Int) {
        observedEntityId.value = entityId

        if (currentComponentIsWatched)
            worldController()?.setComponentStateWatcher(entityId, componentIndex, true)
        else
            worldController()?.requestComponentState(entityId, componentIndex)
    }

    fun viewEntitiesFilters(r: RenderSession) =
        row(arrayOf(text("TODO filters here?")))

    fun viewSystems(): RNode {
        // TODO checkboxes: entity systems, base systems (empty aspectInfo), managers (actives == null)

        val header = tRow(thCell(""), thCell("system"), thCell("entities"), thCell("max entities"))
        val rows = entities().allSystems()
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

    val viewCurrentEntity = renderTo(observedEntityId, currentComponent) { r, entityId, currentComponent ->
        console.log(entityId, currentComponent)
        if (entityId == null) {
            row(
                attrs(width(fillPortion(2)), heightFill),
                row(attrs(widthShrink, centerX), text("Please select any entity..."))
            )
        }
        else {
            val componentName = currentComponent?.componentIndex?.let {
                entities().componentTypes.value[it].name
            }
            row(
                attrs(width(fillPortion(2)), heightFill, spacing(25)),

                column(attrs(alignTop, heightFill, spacing(15)),
                    row(attrs(spacing(5)),
                        row(attrs(borderBottom(1)), text("Entity #$entityId")),
                        button("×") { //TODO show text on hover: "Delete entity #entityId", stylize buttons (no .any class helps)
                            worldController()!!.deleteEntity(entityId)
                        }
                    ),
                    if (currentComponent == null)
                        dummyEl
                    else {
                        val componentIndex = currentComponent.componentIndex

                        labelledCheckbox("Watch E$entityId:C$componentIndex", currentComponentIsWatched) { enabled ->
                            worldController()!!.setComponentStateWatcher(entityId, componentIndex, enabled)
                            currentComponentIsWatched = enabled
                        }
                    },
                    row(
                        attrs(borderBottom(1)),
                        text("Components:")
                    ),
                    viewObservedEntity(r)),
                column(attrs(widthFill, alignTop, spacing(6)),
                    row(attrs(borderBottom(0)),
                        elems(text(componentName?.let {"<$it>:"} ?: "" ))),
                    column(attrs(paddingLeft(treeIndentation)), viewSelectedComponent(r)))
            )
        }
    }.named("viewCurrentEntity")

    val viewObservedEntity = renderTo(observedEntityId, currentComponent) { r, entityId, currentComponent ->
        val componentTypes = entities().entityComponents.value[entityId!!]

        if (componentTypes == null)
            column(
                elems(
                    text("error: component types for entity #$entityId were not found")
                )
            )
        else {
            val componentNames = mutableListOf<RNode>()
            var i: Int = componentTypes.nextSetBit(0)
            while (i >= 0) {
                val cmpType = entities().componentTypes.value[i]
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
    }.named("viewObservedEntity")

    val viewSelectedComponent = currentComponent.renderTo { r, cmp ->
        if (cmp == null)
            column(arrayOf(text("")))
        else {
            val value = cmp.valueTree
            viewValueTree(value.model!!, value, value, true, listOf(cmp.entityId, cmp.componentIndex), 0, listOf(value))
        }
    }.named("viewSelectedComponent")

    val treeSpacing = 3
    val treeNodeHeight = 18
    val treeIndentation = 10

    fun viewValueTree(
        model: ObjectModelNode, value: Any?, rootValue: ValueTree, shouldShowLeafDataTypeIcon: Boolean = true,
        path: List<Int> = listOf(), level: Int = 0, visitedObjects: List<ValueTree> = listOf()
    ): RNode {
        return if (model.isArray) {
            column(attrs(spacing(treeSpacing)),
                row(attrs(height(px(treeNodeHeight))),
                    dataTypeToIcon(DataType.Array, model.isSubTypePrimitive),

                    if (model.isSubTypePrimitive)
                        dataTypeToIcon(model.dataSubType, model.isSubTypePrimitive)
                    else dummyEl,

                    text("${model.name ?: ""} = "),

                    nullCheckbox(value == null,
                        onChange = { isNull ->
                            if (isNull) {
                                currentlyEditedInput = null
                                // TODO this currently would crash since Array is not a flat type!
//                                onValueChanged(rootValue, path, model.dataType, null)
                                notifyCurrentlyEditedInputChanged()
                            }
                        },
                        view = { isNull ->
                            if (isNull) {
                                text("<null>")

                                // TODO add interface that would instantiate an array of specific type and size!
//                                onValueChanged(rootValue, path, model.dataType, newValue)
//                                notifyCurrentlyEditedInputChanged()
                            }
                            else
                                dummyEl
                        }
                    )
                ),
                if (value != null) {
                    val vt = value as ValueTree

                    column(
                        attrs(paddingLeft(3), spacing(treeSpacing)),
                        vt.asIterable().mapIndexed { index, subValue ->
                            val valueModel =
                                (subValue as? ValueTree)?.let {v -> v.model}
                                    ?: model.extractArraySubTypeModel()

                            row(
                                attrs(), elems(
                                    column(attrs(alignTop, paddingTop(2)), text("[$index]")),

                                    // TODO put below into nullCheckbox

                                    if (valueModel.isLeaf || !visitedObjects.contains(subValue)) {
                                        val visited = if (subValue != null && subValue is ValueTree) visitedObjects + subValue else visitedObjects
                                        viewValueTree(valueModel, subValue, rootValue, !model.isSubTypePrimitive, path + index, level + 1, visited)
                                    }
                                    else
                                        text("<rec_obj>")
                                )
                            )
                        }.toTypedArray()
                    )
                } else dummyEl
            )
        }
        else if (model.isLeaf) {
            if (model.isEnum) {
                val enumDescription = model.children!![0]
                val enumTypeName = enumDescription.name!!
                val enumValuesNames = enumDescription.children!!.map { it.name!! }

                row(
                    attrs(height(px(treeNodeHeight))),
                    if (shouldShowLeafDataTypeIcon)
                        dataTypeToIcon(DataType.Enum, false)
                    else dummyEl,
                    text("${model.name ?: ""}<$enumTypeName> = "),
                    dropdown(value as Int?, enumValuesNames, true) {
                        onValueChanged(rootValue, path, model.dataType, it)
                    }
                )
            }
            else if (model.dataType == DataType.Boolean) {
                row(
                    attrs(height(px(treeNodeHeight))),
                    if (shouldShowLeafDataTypeIcon)
                        dataTypeToIcon(DataType.Boolean, model.isTypePrimitive)
                    else dummyEl,
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
                    row(
                        attrs(spacing(treeSpacing)),
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
                        row(
                            attrs(
                                onClick {
                                    currentlyEditedInput = EditedInputState(path, value?.toString() ?: "")
                                    notifyCurrentlyEditedInputChanged()
                                }),
                            text(value?.toString() ?: "<null>")
                        )
                    }
                    else showEditor()

                row(
                    attrs(height(px(treeNodeHeight))),
                    if (shouldShowLeafDataTypeIcon)
                        dataTypeToIcon(model.dataType, model.isTypePrimitive)
                    else dummyEl,
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
        else if (value == null) {
            row(
                attrs(height(px(treeNodeHeight))), elems(
                    dataTypeToIcon(model.dataType, false),
                    text("${model.name ?: ""}: <null>")
                )
            )
        }
        else {
            val vtValues = (value as ValueTree).values
            val fields = model.children!!
                .mapIndexed { i, fieldModel ->
                    val fieldValue: Any? = vtValues[i]

                    if (fieldModel.isLeaf || !visitedObjects.contains(fieldValue)) {
                        val visited = if (fieldValue != null && fieldValue is ValueTree) visitedObjects + fieldValue else visitedObjects
                        viewValueTree(fieldModel, fieldValue, rootValue, true, path + i, level + 1, visited)
                    }
                    else text("<rec_obj>")
                }

            if (level > 0)
                column(
                    attrs(spacing(treeSpacing)),
                    row(
                        attrs(height(px(treeNodeHeight))), elems(
                            dataTypeToIcon(model.dataType, false),
                            text("${model.name ?: ""}:")
                        )
                    ),
                    column(attrs(paddingLeft(treeIndentation), spacing(treeSpacing)), fields.toTypedArray())
                )
            else
                column(attrs(spacing(treeSpacing)), fields.toTypedArray())
        }
    }

    fun onValueChanged(rootValue: ValueTree, path: List<Int>, newValueType: DataType, newValue: Any?) {
        val entityId: Int = path[0]
        val componentIndex: Int = path[1]
        val valueTreePath = path.subList(2, path.size).toIntArray()
        worldController()?.setComponentFieldValue(entityId, componentIndex, valueTreePath, newValueType, newValue)

        // TODO this is just a workaround, we should modify the value in our model instead of refreshing state of whole component. (?)
        worldController()?.requestComponentState(entityId, componentIndex)
        console.log(rootValue, path, newValue)
    }
}