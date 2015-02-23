package net.namekdev.entity_tracker.ui;

import java.awt.EventQueue;

import net.namekdev.entity_tracker.connectors.UpdateListener;

public class StandaloneMain {

	public static void main(String[] args) {
		init();
	}

	public static UpdateListener init() {
		final EntityTrackerMainWindow window = new EntityTrackerMainWindow();

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window.initialize();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		return window;
	}
}
