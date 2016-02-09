package org.jdownloader.iconsetter.gui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.uio.UIOManager;
import org.jdownloader.iconsetter.IconResource;
import org.jdownloader.iconsetter.IconSetMaker;
import org.jdownloader.iconsetter.gui.icon8.Icon8Dialog;

public class SetTable extends BasicJDTable<IconResource> {

    private IconSetMaker owner;

    public SetTable(IconSetMaker owner, SetTableModel setTableModel) {
        super(setTableModel);
        setRowHeight(36);
        this.owner = owner;
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, IconResource contextObject, final List<IconResource> selection, ExtColumn<IconResource> column, MouseEvent mouseEvent) {

        popup.add(new JMenuItem(new BasicAction() {
            {
                setName("Icon8");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                for (IconResource res : selection) {
                    Icon8Dialog d = new Icon8Dialog(res, owner);
                    UIOManager.I().show(null, d);
                    SetTable.this.repaint();

                }
            }
        }));
        return popup;
    }

}
