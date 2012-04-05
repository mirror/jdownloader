package org.jdownloader.gui.views.downloads.table;

import java.awt.Point;

import jd.gui.swing.jdgui.menu.MenuEditor;

public abstract class SubMenuEditor extends MenuEditor {

    abstract public void save();

    public abstract Point getDesiredLocation();

}
