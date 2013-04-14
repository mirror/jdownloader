package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainerRoot;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;

public class ManagerTreeModel extends DefaultTreeModel implements TreeModel {

    private MenuContainerRoot data;

    public static void main(String[] args) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                LookAndFeelController.getInstance().init();
                new ManagerFrame();
            }
        };

    }

    public ManagerTreeModel(MenuContainerRoot menuContainerRoot) {
        super(null, false);
        data = menuContainerRoot;
    }

    @Override
    public Object getRoot() {
        return data;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((MenuItemData) ((MenuItemData) parent).getItems().get(index));
    }

    @Override
    public int getChildCount(Object parent) {
        return ((MenuItemData) parent).getItems() == null ? 0 : ((MenuItemData) parent).getItems().size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return !(((MenuItemData) node).lazyReal() instanceof MenuContainer);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((MenuItemData) parent).getItems().indexOf(child);
    }

    public void moveTo(TreePath obj, MenuItemData parent, int childIndex) {

        MenuItemData itemToMove = (MenuItemData) obj.getLastPathComponent();
        MenuItemData oldParent = ((MenuItemData) obj.getPathComponent(obj.getPathCount() - 2));
        // oldParent.getItems().remove(itemToMove);
        // int oldIndex = oldParent.getItems().indexOf(itemToMove);
        // if(oldIndex>=0)oldParent.getItems().set(oldIndex, element)
        if (childIndex < 0) {
            // dropped on
            oldParent.getItems().remove(itemToMove);
            parent.getItems().add(itemToMove);
        } else {
            List<MenuItemData> a = new ArrayList<MenuItemData>(parent.getItems().subList(0, childIndex));
            List<MenuItemData> b = new ArrayList<MenuItemData>(parent.getItems().subList(childIndex, parent.getItems().size()));

            if (!a.remove(itemToMove)) {
                b.remove(itemToMove);
            }
            ArrayList<MenuItemData> newlist = new ArrayList<MenuItemData>();

            newlist.addAll(a);
            newlist.add(itemToMove);
            newlist.addAll(b);
            parent.setItems(newlist);
            if (oldParent != parent) {
                oldParent.getItems().remove(itemToMove);
            }
        }
        // data
        fireTreeStructureChanged(this, new Object[] { data }, null, null);

    }
}
