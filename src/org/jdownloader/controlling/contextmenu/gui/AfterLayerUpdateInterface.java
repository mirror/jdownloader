package org.jdownloader.controlling.contextmenu.gui;

import javax.swing.JComponent;

import org.jdownloader.controlling.contextmenu.MenuContainer;

public interface AfterLayerUpdateInterface {

    void onAfterLayerDone(JComponent root, MenuContainer md);

}
