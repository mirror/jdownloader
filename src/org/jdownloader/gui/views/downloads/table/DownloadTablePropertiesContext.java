package org.jdownloader.gui.views.downloads.table;

import java.util.ArrayList;

import javax.swing.JMenuItem;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.menu.MenuContext;

public class DownloadTablePropertiesContext extends MenuContext<ArrayList<JMenuItem>> {

    private AbstractNode            clickedObject;
    private ArrayList<AbstractNode> selectedObjects;
    private ExtColumn<AbstractNode> clickedColumn;

    public DownloadTablePropertiesContext(ArrayList<JMenuItem> popup, AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column) {
        super(popup);

        clickedObject = contextObject;
        selectedObjects = selection;
        clickedColumn = column;

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

}
