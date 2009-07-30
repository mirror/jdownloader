package jd.gui.swing.jdgui.actions.event;

import java.util.EventListener;

import jd.gui.swing.jdgui.actions.ActionControlEvent;

public interface ActionControllerListener extends EventListener {

    void onActionControlEvent(ActionControlEvent event);

}
