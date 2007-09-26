package jd.gui.simpleSWT;

import java.util.LinkedList;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

public class ExtendedTree {
    public Tree tree;
    public ExtendedTree(Composite composite, int style) {
       tree=new Tree(composite, style);
       tree.setData(this);
    }
    public ExtendedTreeItem[] getSelection() {

        TreeItem[] items = tree.getSelection();
        ExtendedTreeItem[] returnExtendedTreeItems = new ExtendedTreeItem[items.length];
        for (int i = 0; i < returnExtendedTreeItems.length; i++) {
            returnExtendedTreeItems[i] = (ExtendedTreeItem) items[i].getData();
        }
        return returnExtendedTreeItems;
    }
    /**
     * Gibt die selektierten Items die nicht locked sind und ohne die UnterItems
     * dessen ElternItem selektiert sind aus. Boah was fuer ein komplizierter
     * Satz
     * 
     * @return
     */
    public ExtendedTreeItem[] getOwnSelection() {
        ExtendedTreeItem[] selection = getSelection();
        LinkedList<ExtendedTreeItem> ownselected = new LinkedList<ExtendedTreeItem>();
        for (int i = 0; i < selection.length; i++) {
            if (!selection[i].isParentSelected()) {
                if (!selection[i].isLocked())
                    ownselected.add(selection[i]);
            }
        }
        return (ExtendedTreeItem[]) ownselected.toArray(new ExtendedTreeItem[ownselected.size()]);
    }
    public int getSelectionCount() {
        return tree.getSelectionCount();
    }
    
    public ExtendedTreeItem[] getItems() {
        TreeItem[] items = tree.getItems();
        ExtendedTreeItem[] returnExtendedTreeItems = new ExtendedTreeItem[items.length];
        for (int i = 0; i < returnExtendedTreeItems.length; i++) {
            returnExtendedTreeItems[i] = (ExtendedTreeItem) items[i].getData();
        }
        return returnExtendedTreeItems;
    }
    public ExtendedTreeItem getItem(int i) {
        return (ExtendedTreeItem) tree.getItem(i).getData();
    }
    public void setSelection(ExtendedTreeItem[] items) {
        TreeItem[] selection = new TreeItem[items.length];
        for (int i = 0; i < items.length; i++) {
            selection[i]=items[i].item;
        }
        tree.setSelection(selection);
    }
    public Display getDisplay() {
        return tree.getDisplay();
    }
    public void notifyListeners(int collapse, Event event) {
        tree.notifyListeners(collapse, event);
        
    }
    public void setSelection(ExtendedTreeItem item) {
        tree.setSelection(item.item);
    }
    public Shell getShell() {
        return tree.getShell();
    }
    public void setHeaderVisible(boolean b) {
        tree.setHeaderVisible(b);
        
    }
    public TreeColumn[] getColumns() {
        return tree.getColumns();
    }
    public void setColumnOrder(int[] columnOrder) {
        tree.setColumnOrder(columnOrder);
    }
    public void addListener(int i, Listener listener) {
        tree.addListener(i, listener);
    }
    public void removeListener(int i, Listener listener) {
        tree.removeListener(i, listener);
        
    }

}
