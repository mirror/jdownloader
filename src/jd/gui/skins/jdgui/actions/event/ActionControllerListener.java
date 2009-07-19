package jd.gui.skins.jdgui.actions.event;

import java.util.EventListener;

import jd.gui.skins.jdgui.actions.ActionControlEvent;

public interface ActionControllerListener extends EventListener {

    void onActionControlEvent(ActionControlEvent event);

}
