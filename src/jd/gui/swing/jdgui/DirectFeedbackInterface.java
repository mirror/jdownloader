package jd.gui.swing.jdgui;

import java.awt.Point;

import org.appwork.swing.MigPanel;

public interface DirectFeedbackInterface {

    DirectFeedback layoutDirectFeedback(Point mouse, boolean positive, MigPanel actualContent, VoteFinderWindow window);

}
