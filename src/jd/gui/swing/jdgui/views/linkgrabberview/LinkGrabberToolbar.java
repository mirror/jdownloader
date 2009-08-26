package jd.gui.swing.jdgui.views.linkgrabberview;

import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;

public class LinkGrabberToolbar extends ViewToolbar {

    private static final long serialVersionUID = 1L;

    public LinkGrabberToolbar() {

        setList(new String[] { "action.addurl", "action.load", "action.linkgrabber.addall", "action.linkgrabber.clearlist" });
        BUTTON_CONSTRAINTS = "gaptop 2";
    }

  

    @Override
    public String getButtonConstraint(int i, ToolBarAction action) {
        if (i < 3) {
            return BUTTON_CONSTRAINTS + ", dock west, sizegroup toolbar";
        } else {
            return BUTTON_CONSTRAINTS + ", dock east, sizegroup toolbar";
        }
    }

}
