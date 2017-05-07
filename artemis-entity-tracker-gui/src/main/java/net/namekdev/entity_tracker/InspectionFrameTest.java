package net.namekdev.entity_tracker;

import net.namekdev.entity_tracker.ui.utils.InspectionTreeComponent;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public class InspectionFrameTest {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(800, 600);
        frame.setLocation(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        InspectionTreeComponent inspectionFrame = new InspectionTreeComponent();
        frame.add(new JScrollPane(inspectionFrame));

        frame.setVisible(true);
    }
}

