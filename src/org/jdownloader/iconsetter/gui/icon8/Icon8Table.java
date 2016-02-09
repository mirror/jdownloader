package org.jdownloader.iconsetter.gui.icon8;

import org.jdownloader.iconsetter.IconSetMaker;
import org.jdownloader.iconsetter.gui.Icon8Resource;

import jd.gui.swing.jdgui.BasicJDTable;

public class Icon8Table extends BasicJDTable<Icon8Resource> {

    private IconSetMaker owner;
    private Icon8Dialog  icon8Dialog;

    public Icon8Table(IconSetMaker owner, Icon8Dialog icon8Dialog, Icon8TableModel setTableModel) {
        super(setTableModel);
        setRowHeight(36);
        this.icon8Dialog = icon8Dialog;
        this.owner = owner;
    }

}
