package net.namekdev.entity_tracker;

import net.namekdev.entity_tracker.ui.utils.InspectionTreeComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 *
 */
public class InspectionFrameTest {

    public static void main(String[] args) {
        final JFrame frame = new JFrame();
        frame.setSize(800, 600);
        frame.setLocation(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        InspectionTreeComponent inspectionFrame = new InspectionTreeComponent();
        JScrollPane scrollPane = new JScrollPane(inspectionFrame);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(scrollPane);

        frame.setVisible(true);

        frame.getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
}

