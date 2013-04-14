package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.tree.TreePath;

import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;

public class MenuItemTransferAble implements Transferable {
    public final static DataFlavor NODE_FLAVOR = new DataFlavor(MenuItemData.class, "MenuItem");
    private TreePath               path;

    public MenuItemTransferAble(TreePath path) {
        this.path = path;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        System.out.println("GET");
        return new DataFlavor[] { NODE_FLAVOR };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor == NODE_FLAVOR;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        return path;
    }

}
