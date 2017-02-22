package net.namekdev.entity_tracker.ui.listener

import net.namekdev.entity_tracker.ui.model.BaseSystemTableModel

interface ChangingSystemEnabledStateListener : java.util.EventListener {
    fun onChangingSystemEnabledState(model: BaseSystemTableModel, systemIndex: Int, systemName: String, enabled: Boolean)
}
