package jd.gui.swing.jdgui;

import java.awt.Point;

import org.appwork.swing.MigPanel;

public interface DirectFeedbackInterface {

    DirectFeedback layoutDirectFeedback(Point mouse, MigPanel actualContent, AbstractBugFinderWindow window);

}
