package org.jdownloader.iconsetter.gui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.iconsetter.IconResource;
import org.jdownloader.iconsetter.IconSetMaker;
import org.jdownloader.iconsetter.gui.icon8.Icon8Dialog;

import jd.gui.swing.jdgui.BasicJDTable;

public class SetTable extends BasicJDTable<IconResource> {

    private IconSetMaker owner;

    @Override
    public SetTableModel getModel() {
        return (SetTableModel) super.getModel();
    }

    public SetTable(IconSetMaker owner, SetTableModel setTableModel) {
        super(setTableModel);
        setRowHeight(36);
        this.owner = owner;
        setSearchEnabled(true);
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, IconResource obj) {
        Icon8Dialog d = new Icon8Dialog(obj, owner);
        UIOManager.I().show(null, d);
        SetTable.this.repaint();
        return true;
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, IconResource contextObject, final List<IconResource> selection, ExtColumn<IconResource> column, MouseEvent mouseEvent) {

        popup.add(new JMenuItem(new BasicAction() {
            {
                setName("Icon8 Lookup");
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
        final File svg = contextObject.getFile(getModel().getSelected(), "svg");
        if (svg.exists()) {
            popup.add(new JMenuItem(new BasicAction() {
                {
                    setName("Show in Explorer (SVG)");
                }

                @Override
                public void actionPerformed(ActionEvent e) {

                    CrossSystem.showInExplorer(svg);

                }
            }));

            popup.add(new JMenuItem(new BasicAction() {
                {
                    setName("Edit File (SVG)");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(svg);

                }
            }));
        }

        final File png = contextObject.getFile(getModel().getSelected(), "png");
        if (png.exists()) {
            popup.add(new JMenuItem(new BasicAction() {
                {
                    setName("Show in Explorer (PNG)");
                }

                @Override
                public void actionPerformed(ActionEvent e) {

                    CrossSystem.showInExplorer(png);

                }
            }));
            popup.add(new JMenuItem(new BasicAction() {
                {
                    setName("Edit File (PNG)");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(png);

                }
            }));
        }

        return popup;
    }

}
