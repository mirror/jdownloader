package org.jdownloader.gui.views.downloads.table;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.menu.MenuContext;

public class CopyOfDownloadTableDownloadLinkPropertiesContext extends MenuContext<JPopupMenu> {

    private DownloadsTable          table;
    private AbstractNode            clickedObject;
    private ArrayList<AbstractNode> selectedObjects;
    private ExtColumn<AbstractNode> clickedColumn;
    private MouseEvent              mouseEvent;

    public CopyOfDownloadTableDownloadLinkPropertiesContext(DownloadsTable downloadsTable, JPopupMenu popup, AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent ev) {
        super(popup);
        table = downloadsTable;
        clickedObject = contextObject;
        selectedObjects = selection;
        clickedColumn = column;
        mouseEvent = ev;

    }

    public DownloadsTable getTable() {
        return table;
    }

    public AbstractNode getClickedObject() {
        return clickedObject;
    }

    public ArrayList<AbstractNode> getSelectedObjects() {
        return selectedObjects;
    }

    public ExtColumn<AbstractNode> getClickedColumn() {
        return clickedColumn;
    }

    public MouseEvent getMouseEvent() {
        return mouseEvent;
    }

}
