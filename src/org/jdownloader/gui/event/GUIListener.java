package org.jdownloader.gui.event;

import java.util.EventListener;

import jd.gui.swing.jdgui.interfaces.View;

public interface GUIListener extends EventListener {

    void onGuiMainTabSwitch(View oldView, View newView);

    void onKeyModifier(int parameter);

}