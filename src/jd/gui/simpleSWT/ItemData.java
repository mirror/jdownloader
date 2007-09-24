package jd.gui.simpleSWT;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ItemData {
    /**
     * Achtung wenn ein neuer TYPE gesetzt wird muss dafuer in der
     * jd.JDSWTUtilities.java die Methode getTreeImage gegebenenfalls angepasst
     * werden und neue Bilder in der MainGui.java Methode loadImages() geladen
     * werden
     */
    public final static int STATUS_NOTHING = 1;
    public final static int STATUS_ERROR = 2;
    public final static int STATUS_QUEUE = 3;
    public final static int STATUS_DOWNLOADING = 4;
    public final static int TYPE_FILE = 1;
    public final static int TYPE_FOLDER = 2;
    public final static int TYPE_CONTAINER = 3;
    public final static int TYPE_LOCKEDCONTAINER = 4;
    public final static int TYPE_DEFAULT = 5;

    public int status = STATUS_NOTHING;
    public int type = TYPE_DEFAULT;
    public boolean isLocked = false;
    public String link = null;
    private TreeEditor editor = null;
    private ProgressBar progressBar = null;
    /**
     * @return the editor
     */
    public ProgressBar getProgressBar() {
        return progressBar;
    }
    public ProgressBar setProgressBar(final TreeItem treeItem) {
        final Tree tree = treeItem.getParent();
        progressBar = new ProgressBar(tree, SWT.SMOOTH);
        final Listener listener = new Listener() {

            public void handleEvent(Event e) {
                if (e.stateMask == SWT.CTRL && tree.getSelectionCount() > 0) {
                    Event event = new Event();
                    event.widget = tree;
                    int x = 0;
                    int[] c = tree.getColumnOrder();
                    for (int i = 0; i < c.length; i++) {
                        if (c[i] == 2)
                            break;
                        x += tree.getColumn(c[i]).getWidth();

                    }

                    event.x = x + e.x;
                    event.y = treeItem.getBounds().y + 5;
                    event.button = 1;
                    TreeItem[] items = tree.getSelection();
                    TreeItem[] items2;
                    boolean isSelected = false;
                    for (int i = 0; i < items.length; i++) {
                        if (items[i] == treeItem) {
                            isSelected = true;
                            break;
                        }
                    }

                    if (isSelected) {
                        if(e.button != 1 && tree.getSelectionCount() > 1)
                            return;
                        items2 = new TreeItem[items.length - 1];
                        int d = 0;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] != treeItem) 
                                items2[d++] = items[i];
                        }
                    } else {
                        items2 = new TreeItem[items.length + 1];
                        items2[0] = treeItem;
                        for (int i = 1; i < items2.length; i++) {
                            items2[i] = items[i - 1];
                        }
                    }
                    // tree.setSelection(items);
                    tree.setSelection(items2);
                    tree.notifyListeners(SWT.MouseDown, event);
                    GuiListeners.getListener("toolBarBtSetEnabled").handleEvent(e);
                } else {
                    if(e.button != 1 && tree.getSelectionCount() > 1)
                    {
                        TreeItem[] items = tree.getSelection();
                        boolean isSelected = false;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == treeItem) {
                                isSelected = true;
                                break;
                            }
                        }
                        if(isSelected) return; 
                    }
                    Event event = new Event();
                    event.widget = tree;
                    int x = 0;
                    int[] c = tree.getColumnOrder();
                    for (int i = 0; i < c.length; i++) {
                        if (c[i] == 2)
                            break;
                        x += tree.getColumn(c[i]).getWidth();

                    }
                    event.x = x + e.x;
                    event.y = treeItem.getBounds().y + 5;
                    event.button = 1;
                    tree.notifyListeners(SWT.MouseDown, event);
                    tree.setSelection(treeItem);
                    GuiListeners.getListener("toolBarBtSetEnabled").handleEvent(e);
                }

            }

        };

        progressBar.addListener(SWT.MouseDown, listener);

        progressBar.setMenu(tree.getMenu());
        editor = new TreeEditor(tree);
        editor.grabHorizontal = editor.grabVertical = true;
        editor.setEditor(progressBar, treeItem, 2);
        return progressBar;
    }
    public void disposeProgressBar() {
        if (editor != null)
            editor.dispose();
        if (progressBar != null)
            progressBar.dispose();
        progressBar = null;
        editor = null;

    }
}
