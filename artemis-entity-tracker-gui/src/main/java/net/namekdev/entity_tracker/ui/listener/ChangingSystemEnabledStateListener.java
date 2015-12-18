package net.namekdev.entity_tracker.ui.listener;

import net.namekdev.entity_tracker.ui.model.BaseSystemTableModel;

public interface ChangingSystemEnabledStateListener extends java.util.EventListener {
	void onChangingSystemEnabledState(BaseSystemTableModel model, int systemIndex, String systemName, boolean enabled);
}
