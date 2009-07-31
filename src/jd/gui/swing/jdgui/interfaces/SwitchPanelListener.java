package jd.gui.swing.jdgui.interfaces;

import java.util.EventListener;

/**
 * GFets informed about events in a SwitchPanel
 * @author Coalado
 *
 */
public abstract class SwitchPanelListener implements EventListener {

    abstract public void onPanelEvent(SwitchPanelEvent event);

}
