package jd.gui.simpleSWT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

public class DownloadTab {

    public Tree trDownload;
    /**
     * Geghoert zum Drag&Drop System
     */
    private Object dragSourceItem = null;
    private Object dataMap = new Object();
    private MainGui mainGui;
    public TreeItem ownSelected = null;
    /**
     * Liste Aller TreeEditoren
     */

    private int lastStep = 0;
    private int lastTime = (int) System.currentTimeMillis();
    private Text renameText;
    public DownloadTab(MainGui mainGui) {
        this.mainGui = mainGui;
        initDownload();
    }
    private void initDownload() {
        CTabItem tbDownloadFiles = new CTabItem(mainGui.folder, SWT.NONE);
        tbDownloadFiles.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.name"));
        tbDownloadFiles.setImage(JDSWTUtilities.getImageSwt("download"));
        /**
         * Listeners
         */
        Listener newFolderListener = new Listener() {
            public void handleEvent(Event event) {
                /**
                 * TODO
                 */
                String[] text = {JDSWTUtilities.getSWTResourceString("DownloadTab.newFolder.name"), "", ""};

                if (trDownload.getSelectionCount() > 0) {
                    TreeItem[] items = trDownload.getItems();
                    int index = 0;
                    TreeItem item = trDownload.getSelection()[0];

                    TreeItem parent = item.getParentItem();
                    if (parent != null)
                        while (parent != null) {
                            TreeItem parent2 = parent.getParentItem();
                            if (parent2 != null)
                                parent = parent2;
                            else {
                                for (int i = 0; i < items.length; i++) {
                                    if (items[i] == parent) {
                                        index = i;
                                        parent = null;
                                        break;
                                    }
                                }
                            }
                        }

                    else {
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == item) {
                                index = i;
                                break;
                            }
                        }
                    }

                    trDownload.setSelection(JDSWTUtilities.createTreeitem(trDownload, text, null, ItemData.STATUS_NOTHING, ItemData.TYPE_FOLDER, index));
                    // workaround fuer die scheiss treeEditoren
                    // laesst die treeEditoren neu zeichnen
                    trDownload.notifyListeners(SWT.Collapse, new Event());
                } else {

                    trDownload.setSelection(JDSWTUtilities.createTreeitem(trDownload, text, null, ItemData.STATUS_NOTHING, ItemData.TYPE_FOLDER));

                    // muss hier nicht gemacht werden, da das item
                    // sowieso an letzter stelle hinzugefuegt
                    // wird
                }
                mainGui.guiListeners.getListener("DownloadTab.rename").handleEvent(new Event());

            }

        };
        mainGui.guiListeners.addListener("DownloadTab.newFolder", newFolderListener);

        Listener newContainerListener = new Listener() {
            public void handleEvent(Event event) {
                /**
                 * TODO
                 */
                String[] text = {JDSWTUtilities.getSWTResourceString("DownloadTab.newContainer.name"), "", ""};

                if (trDownload.getSelectionCount() > 0) {
                    TreeItem[] items = trDownload.getItems();
                    int index = 0;
                    TreeItem item = trDownload.getSelection()[0];

                    TreeItem parent = item.getParentItem();
                    if (parent != null)
                        while (parent != null) {
                            TreeItem parent2 = parent.getParentItem();
                            if (parent2 != null)
                                parent = parent2;
                            else {
                                for (int i = 0; i < items.length; i++) {
                                    if (items[i] == parent) {
                                        index = i;
                                        parent = null;
                                        break;
                                    }
                                }
                            }
                        }

                    else {
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == item) {
                                index = i;
                                break;
                            }
                        }
                    }

                    trDownload.setSelection(JDSWTUtilities.createTreeitem(trDownload, text, null, ItemData.STATUS_NOTHING, ItemData.TYPE_CONTAINER, index));
                    // workaround fuer die scheiss treeEditoren
                    // laesst die treeEditoren neu zeichnen
                    trDownload.notifyListeners(SWT.Collapse, new Event());
                } else {

                    trDownload.setSelection(JDSWTUtilities.createTreeitem(trDownload, text, null, ItemData.STATUS_NOTHING, ItemData.TYPE_CONTAINER));

                    // muss hier nicht gemacht werden, da das item
                    // sowieso an letzter stelle hinzugefuegt
                    // wird
                }
                mainGui.guiListeners.getListener("DownloadTab.rename").handleEvent(new Event());

            }

        };
        mainGui.guiListeners.addListener("DownloadTab.newContainer", newContainerListener);

        Listener deleteListener = new Listener() {
            public void handleEvent(Event event) {
                TreeItem[] items = getSelection(trDownload);
                MessageBox mbDelete = new MessageBox(trDownload.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                String itemsText = "";
                if (items.length < 30) {
                    for (int i = 0; i < items.length; i++) {
                        itemsText += System.getProperty("line.separator") + items[i].getText();
                    }
                    mbDelete.setMessage(JDSWTUtilities.getSWTResourceString("mbDelete.message").replace("%s", itemsText));
                } else
                    itemsText = JDSWTUtilities.getSWTResourceString("mbDelete.messageOver30").replace("%s", itemsText);

                mbDelete.setText(JDSWTUtilities.getSWTResourceString("mbDelete.name"));
                int response = mbDelete.open();
                if (response == SWT.YES) {
                    for (int i = 0; i < items.length; i++) {
                        JDSWTUtilities.disposeItem(items[i]);
                    }
                    trDownload.notifyListeners(SWT.Collapse, new Event());
                }

            }
        };
        mainGui.guiListeners.addListener("DownloadTab.delete", deleteListener);

        Listener goUpListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(1);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goUp", goUpListener);
        Listener goDownListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(2);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goDown", goDownListener);
        Listener goLastUpListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(3);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goLastUp", goLastUpListener);
        Listener goLastDownListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(4);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goLastDown", goLastDownListener);
        /**
         * Listenersende
         */
        CTabFolder folder = tbDownloadFiles.getParent();
        final Shell shell = folder.getShell();
        trDownload = new Tree(folder, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        tbDownloadFiles.setControl(trDownload);
        trDownload.setHeaderVisible(true);
        /*
         * Workearound fuer ColumnReorder
         */
        Listener downloadColumnListener = new Listener() {

            public void handleEvent(Event e) {
                System.out.println(e);
                lastStep = 0;
                lastTime = e.time + 5000;
            }

        };
        Listener downloadColumnMoveListener = new Listener() {

            public void handleEvent(final Event e) {
                if (e.time == 0)
                    return;
                if (lastStep == 0) {
                    if (e.time < lastTime + 100)
                        return;
                    lastStep++;
                    lastTime = e.time;
                } else {
                    lastStep++;
                    lastTime = e.time;
                    if ((lastStep > 2)) {
                        lastStep = 0;
                        return;
                    }

                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(2);
                            } catch (InterruptedException e) {
                            }
                            trDownload.getDisplay().syncExec(new Runnable() {
                                public void run() {
                                    trDownload.setHeaderVisible(false);
                                    trDownload.setHeaderVisible(true);

                                }
                            });

                        }
                    }.start();

                }

            }

        };

        Listener downloadColumnResize = new Listener() {

            public void handleEvent(Event e) {
                lastTime = e.time;
                TreeColumn[] columns = trDownload.getColumns();
                int[] DownloadColumnWidht = new int[columns.length];
                for (int i = 0; i < (columns.length); i++) {
                    DownloadColumnWidht[i] = columns[i].getWidth();
                }
                if (shell.getMaximized() == true)
                    mainGui.guiConfig.DownloadColumnWidhtMaximized = DownloadColumnWidht;
                else
                    mainGui.guiConfig.DownloadColumnWidht = DownloadColumnWidht;
                DownloadColumnWidht = null;

            }

        };

        for (int i = 0; i < mainGui.guiConfig.DownloadColumnWidht.length; i++) {
            TreeColumn col = JDSWTUtilities.treeCol(JDSWTUtilities.getSWTResourceString("DownloadTab.column" + i + ".name"), trDownload, mainGui.guiConfig.DownloadColumnWidht[i]);
            col.setResizable(true);
            col.setMoveable(true);
            col.addListener(SWT.Resize, downloadColumnResize);
            col.addListener(SWT.Move, downloadColumnMoveListener);
            col.addListener(SWT.Selection, downloadColumnListener);
        }
        trDownload.setColumnOrder(mainGui.guiConfig.DownloadColumnOrder);
        treeAddDragAndDrop(trDownload);
        // Create the editor and set its attributes
        final TreeEditor editor = new TreeEditor(trDownload);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        mainGui.guiListeners.addListener("DownloadTab.trDownload_MouseDown", new Listener() {

            public void handleEvent(Event e) {
                if (trDownload.getSelectionCount() == 1) {
                    TreeItem item = trDownload.getSelection()[0];
                    if (((ItemData) item.getData()).isLocked)
                        ownSelected = null;
                    else if (ownSelected != null && ownSelected == item) {
                        Point pt = new Point(e.x, e.y);
                        Rectangle rect = item.getBounds(0);
                        rect.x += 24;
                        rect.width -= 24;
                        if (rect.contains(pt) && e.button == 1) {
                            mainGui.guiListeners.getListener("DownloadTab.rename").handleEvent(e);
                            ownSelected = null;
                        } else
                            ownSelected = item;
                    } else
                        ownSelected = item;
                } else {
                    ownSelected = null;
                }
            }

        });
        trDownload.addListener(SWT.MouseDown, mainGui.guiListeners.getListener("DownloadTab.trDownload_MouseDown"));
        mainGui.guiListeners.addListener("DownloadTab.rename", new Listener() {

            public void handleEvent(Event event) {
                // Make sure one and only one item is selected when F2 is
                // pressed

                // Determine the item to edit
                TreeItem[] tItems = getSelection(trDownload);
                if (tItems.length < 1)
                    return;
                final TreeItem item = tItems[0];
                trDownload.setSelection(item);

                if (item == null || item.isDisposed())
                    return;
                // Create a text field to do the editing
                renameText = new Text(trDownload, SWT.NONE);
                renameText.setText(item.getText(0));
                renameText.selectAll();
                renameText.setFocus();

                // If the text field loses focus, set its text into the tree
                // and end the editing session

                renameText.addFocusListener(new FocusAdapter() {
                    public void focusLost(FocusEvent event) {
                        JDSWTUtilities.redrawTreeImage(item, renameText.getText());
                        item.setText(0, renameText.getText());
                        renameText.dispose();
                    }
                });
                final Listener mouseDown = new Listener() {

                    public void handleEvent(Event e) {
                        Point pt = new Point(e.x, e.y);
                        Rectangle rect = item.getBounds(0);
                        rect.x += 24;
                        rect.width -= 24;
                        if (!rect.contains(pt)) {
                            renameText.notifyListeners(SWT.FocusOut, new Event());
                        }
                    }

                };
                trDownload.addListener(SWT.MouseDown, mouseDown);

                // If they hit Enter, set the text into the tree and end the
                // editing
                // session. If they hit Escape, ignore the text and end the
                // editing
                // session

                renameText.addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent event) {
                        switch (event.keyCode) {
                            case SWT.CR :
                                // Enter hit--set the text into the tree and
                                // drop through
                                JDSWTUtilities.redrawTreeImage(item, renameText.getText());
                                item.setText(0, renameText.getText());
                            case SWT.ESC :
                                // End editing session
                                renameText.dispose();
                                break;
                        }
                    }
                });

                renameText.addListener(SWT.Dispose, new Listener() {

                    public void handleEvent(Event arg0) {
                        trDownload.removeListener(SWT.MouseDown, mouseDown);

                    }

                });

                editor.setEditor(renameText, item);

            }
        });

        /**
         * Kontextmenu fuer die TreeItems
         */
        Menu menu = new Menu(shell, SWT.POP_UP);
        trDownload.setMenu(menu);
        /*
         * Menu zum erstellen neuer Ordner
         */
        MenuItem itemNewFolder = new MenuItem(menu, SWT.PUSH);
        itemNewFolder.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.itemNewFolder.name"));
        itemNewFolder.addListener(SWT.Selection, newFolderListener);

        /*
         * Menu zum erstellen neuer Container
         */
        MenuItem itemNewContainer = new MenuItem(menu, SWT.PUSH);
        itemNewContainer.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.itemNewContainer.name"));
        itemNewContainer.addListener(SWT.Selection, newContainerListener);
        trDownload.setMenu(menu);

        new MenuItem(menu, SWT.SEPARATOR);

        /*
         * Menu zum Kopieren von Items
         */
        final MenuItem itemCopy = new MenuItem(menu, SWT.PUSH);
        itemCopy.setText(JDSWTUtilities.getSWTResourceString("itemCopy.name"));
        itemCopy.addListener(SWT.Selection, mainGui.guiListeners.addTreeCopyListener(trDownload, "DownloadTab.copy"));

        /*
         * Menu zum loeschen von Items
         */
        final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
        itemDelete.setText(JDSWTUtilities.getSWTResourceString("itemDelete.name"));
        itemDelete.addListener(SWT.Selection, deleteListener);

        /*
         * Menu zum umbennen von Items
         */
        final MenuItem itemRename = new MenuItem(menu, SWT.PUSH);
        itemRename.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.itemRename.name"));
        itemRename.addListener(SWT.Selection, mainGui.guiListeners.getListener("DownloadTab.rename"));

        new MenuItem(menu, SWT.SEPARATOR);
        MenuItem itemSelectAll = new MenuItem(menu, SWT.PUSH);
        itemSelectAll.setText(JDSWTUtilities.getSWTResourceString("itemSelectAll.name"));
        itemSelectAll.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                trDownload.selectAll();

            }
        });

        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(org.eclipse.swt.events.MenuEvent e) {
                boolean isSelected = trDownload.getSelectionCount() > 0;
                itemDelete.setEnabled(isSelected);
                itemRename.setEnabled(getSelection(trDownload).length > 0);
                itemCopy.setEnabled(isSelected);

            }
        });

        {
            String[] text2 = {"Archiv.php", "", "", ""};

            JDSWTUtilities.setTreeItemDownloading(JDSWTUtilities.createTreeitem(trDownload, text2, null, ItemData.STATUS_NOTHING, ItemData.TYPE_FILE), mainGui.guiListeners);

            JDSWTUtilities.setTreeItemDownloading(JDSWTUtilities.createTreeitem(trDownload, text2, null, ItemData.STATUS_NOTHING, ItemData.TYPE_FILE), mainGui.guiListeners);

            for (int i = 1; i < 5; i++) {
                String[] text = {"" + i, "", "", ""};
                JDSWTUtilities.createTreeitem(trDownload, text, "http://google.de", ItemData.STATUS_NOTHING, i);
            }
            String[] text3 = {"rar", "php", "exe", "jar", "jpg", "txt"};
            for (int i = 0; i < text3.length; i++) {
                JDSWTUtilities.createTreeitem(trDownload, new String[]{"file" + i + "." + text3[i], "", "", ""}, "http://google.de", ItemData.STATUS_NOTHING, ItemData.TYPE_FILE);
            }

        }
        trDownload.addListener(SWT.Selection, mainGui.guiListeners.initToolBarBtSetEnabledListener());
        trDownload.addListener(SWT.Collapse, mainGui.guiListeners.getListener("toolBarBtSetEnabled"));
        trDownload.addKeyListener(mainGui.guiListeners.initTrDownloadKeyListener());
    }
    /**
     * Checkt ob das Item Selektiert ist
     * 
     * @param item
     * @param selection
     * @return
     */
    private boolean isSelected(TreeItem item, TreeItem[] selection) {
        for (int i = 0; i < selection.length; i++) {
            if (item == selection[i])
                return true;
        }
        return false;
    }
    /**
     * Checkt ob ein ElternItem selektiert ist
     * 
     * @param item
     * @return
     */
    private static boolean isParentSelected(TreeItem item) {
        TreeItem[] selection = item.getParent().getSelection();
        TreeItem parent = item.getParentItem();
        while (parent != null) {
            for (int i = 0; i < selection.length; i++) {
                if (parent == selection[i])
                    return true;
            }
            parent = parent.getParentItem();
        }
        return false;
    }
    /**
     * Gibt die selektierten Items ohne die selektierten UnterItems dessen
     * ElternItem selektiert sind aus. Boah was fuer ein komplizierter Satz
     * 
     * @param tree
     * @return
     */
    static TreeItem[] getSelection(Tree tree) {
        TreeItem[] selection = tree.getSelection();
        ArrayList<TreeItem> ownselected = new ArrayList<TreeItem>();
        for (int i = 0; i < selection.length; i++) {
            if (!isParentSelected(selection[i])) {
                if (!((ItemData) selection[i].getData()).isLocked)
                    ownselected.add(selection[i]);
            }
        }
        return (TreeItem[]) ownselected.toArray(new TreeItem[ownselected.size()]);
    }
    /**
     * Diese Methode ist fuer die Buttons in der Toolbar die die Items
     * verschieben es gibt vier Positionen zwischen denen man waehlen kann
     * 1=GoUP 2=GoDown 3=GoLastUp 4=GoLastDown
     * 
     * @param pos
     */
    private void goTo(int pos) {
        if ((renameText != null) && !renameText.isDisposed())
            renameText.dispose();
        int pos2;
        if (pos == 1) {
            pos2 = 0;
            pos = -1;
        } else
            pos2 = 1;
        HashMap<TreeItem, Integer> lostItems = new HashMap<TreeItem, Integer>();
        HashMap<TreeItem, Integer> addItems = new HashMap<TreeItem, Integer>();

        int toTree = 0;
        TreeItem[] items = getSelection(trDownload);
        TreeItem[] itemsToSelect = new TreeItem[items.length];
        DataMap[] dat = (DataMap[]) new DataMap[items.length];
        for (int i = 0; i < items.length; i++) {
            int cc = i;
            if (pos == 2 || pos == 3)
                cc = items.length - i - 1;
            dat[i] = getItemData(items[cc], trDownload.getColumnCount());
            if (pos < 3) {
                TreeItem parent = items[cc].getParentItem();
                if (parent != null) {
                    TreeItem[] items2 = parent.getItems();
                    int index = 0;
                    for (int b = 0; b < items2.length; b++) {
                        if (items2[b] == items[cc]) {
                            index = b;
                            break;
                        }
                    }

                    if (pos < 0 & index > 0 || pos > 0 & index != (items2.length - 1)) {
                        if (pos == 2) {
                            while (isSelected(parent.getItem(index + 1), items))
                                index++;

                            if (lostItems.containsKey(parent))
                                index = index - ((Integer) lostItems.get(parent));
                        }
                        itemsToSelect[i] = setDataMapItems(parent, dat[i], index + pos);
                    } else {
                        TreeItem parent2 = parent.getParentItem();
                        if (parent2 == null)
                            items2 = trDownload.getItems();
                        else
                            items2 = parent2.getItems();

                        index = 0;
                        for (int b = 0; b < items2.length; b++) {
                            if (items2[b] == parent) {
                                index = b;
                                break;
                            }
                        }
                        if (pos == 2)
                            if (lostItems.containsKey(parent)) {
                                Integer inte = ((Integer) lostItems.get(parent));
                                lostItems.remove(parent);
                                lostItems.put(parent, inte++);
                            } else {
                                lostItems.put(parent, 1);
                            }
                        if (parent2 == null) {
                            if (pos == 2)
                                index = index + toTree;
                            itemsToSelect[i] = setDataMapItems(trDownload, dat[i], index + pos2);
                            toTree++;
                        } else {

                            if (pos == 2)
                                if (addItems.containsKey(parent2)) {
                                    Integer inte = ((Integer) addItems.get(parent2));
                                    index = index + inte;
                                    addItems.remove(parent2);
                                    addItems.put(parent2, inte++);
                                } else {
                                    addItems.put(parent2, 1);
                                }
                            itemsToSelect[i] = setDataMapItems(parent2, dat[i], index + pos2);

                        }

                    }
                } else {
                    TreeItem[] items2 = trDownload.getItems();
                    int index = 0;
                    for (int b = 0; b < items2.length; b++) {
                        if (items2[b] == items[cc]) {
                            index = b;
                            break;
                        }
                    }
                    if (pos == 2)
                        while (isSelected(trDownload.getItem(index + 1), items))
                            index++;
                    itemsToSelect[i] = setDataMapItems(trDownload, dat[i], index + pos + toTree);
                }
            } else if (pos == 3) {
                itemsToSelect[i] = setDataMapItems(trDownload, dat[i], 0);
            } else {
                itemsToSelect[i] = setDataMapItems(trDownload, dat[i], -1);
            }
            JDSWTUtilities.disposeItem(items[cc]);
        }
        trDownload.setSelection(itemsToSelect);
        trDownload.notifyListeners(SWT.Collapse, new Event());

    }
    public boolean isDragItem(TreeItem item, TreeItem[] dragSourceItems) {
        if (isSelected(item, dragSourceItems))
            return true;
        else {
            TreeItem parent = item.getParentItem();
            if (parent != null)
                if (isDragItem(parent, dragSourceItems))
                    return true;
        }
        return false;
    }
    // public void moveItem()
    /**
     * Hier faengt das Drag&Drop System an und meine Dokumentation hoert auf
     * denn das will sich sowieso keiner antun
     */
    private void treeAddDragAndDrop(final Tree tree) {
        Transfer[] types = new Transfer[]{TextTransfer.getInstance()};
        int operations = DND.DROP_MOVE;
        final DragSource source = new DragSource(tree, operations);
        source.setTransfer(types);
        source.addDragListener(new DragSourceListener() {
            public void dragStart(DragSourceEvent event) {
                if ((renameText != null) && !renameText.isDisposed())
                    renameText.dispose();
                event.doit = true;
                TreeItem[] selection = getSelection(tree);
                if (selection.length == 0) {
                    event.doit = false;
                }

                dragSourceItem = selection;

            };

            public void dragSetData(DragSourceEvent event) {

                TreeItem[] items = (TreeItem[]) dragSourceItem;
                DataMap[] dat = (DataMap[]) new DataMap[items.length];
                String evd = "";
                for (int i = 0; i < items.length; i++) {
                    dat[i] = getItemData(items[i], tree.getColumnCount());
                    ItemData itd = (ItemData) items[i].getData();
                    if (itd.link != null)
                        evd += itd.link + ((i != items.length - 1) ? System.getProperty("line.separator") : "");
                    else
                        evd += items[i].getText() + ((i != items.length - 1) ? System.getProperty("line.separator") : "");
                }

                event.data = evd;
                dataMap = dat;
            }

            public void dragFinished(DragSourceEvent event) {

                TreeItem[] items = (TreeItem[]) dragSourceItem;
                if (event.detail == DND.DROP_MOVE)
                    for (int i = 0; i < items.length; i++) {
                        if (items[i] != null)
                            JDSWTUtilities.disposeItem(items[i]);
                    }
                dragSourceItem = null;
            }
        });

        DropTarget target = new DropTarget(tree, operations);
        target.setTransfer(types);

        target.addDropListener(new DropTargetAdapter() {
            public void dragOver(DropTargetEvent event) {
                event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
                if (event.item != null) {
                    TreeItem item = (TreeItem) event.item;
                    Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
                    Rectangle bounds = item.getBounds();
                    if (pt.y < bounds.y + bounds.height / 3) {
                        event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
                    } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                        event.feedback |= DND.FEEDBACK_INSERT_AFTER;
                    } else {
                        event.feedback |= DND.FEEDBACK_SELECT;
                    }
                }
            }

            public void drop(DropTargetEvent event) {
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                }
                if (dragSourceItem == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                }
                if (event.item == null) {
                    TreeItem[] selectItems = new TreeItem[((DataMap[]) dataMap).length];
                    for (int i = 0; i < ((DataMap[]) dataMap).length; i++) {
                        selectItems[i] = setDataMapItems(tree, ((DataMap[]) dataMap)[i], -1);
                    }
                    tree.setSelection(selectItems);
                    selectItems = null;

                } else {
                    TreeItem item = (TreeItem) event.item;
                    boolean isContainer = ((ItemData) item.getData()).type == ItemData.TYPE_CONTAINER;
                    LinkedList<TreeItem> selectItems = new LinkedList<TreeItem>();
                    if (isDragItem(item, (TreeItem[]) dragSourceItem)) {
                        event.detail = DND.DROP_NONE;
                        return;
                    }
                    Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
                    Rectangle bounds = item.getBounds();
                    TreeItem parent = item.getParentItem();
                    if (parent != null) {
                        TreeItem[] items = parent.getItems();
                        int index = 0;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == item) {
                                index = i;
                                break;
                            }
                        }

                        if (pt.y < bounds.y + bounds.height / 3) {
                            for (int i = 0; i < ((DataMap[]) dataMap).length; i++) {
                                selectItems.add(setDataMapItems(parent, ((DataMap[]) dataMap)[i], index));
                            }
                            tree.setSelection(selectItems.toArray(new TreeItem[selectItems.size()]));
                            selectItems = null;
                        } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                            if (((ItemData) item.getData()).type == ItemData.TYPE_FOLDER | isContainer) {

                                for (int i = 0; i < ((DataMap[]) dataMap).length; i++) {
                                    if (!isContainer || ((ItemData) ((TreeItem[]) dragSourceItem)[i].getData()).type == ItemData.TYPE_FILE)
                                        selectItems.add(setDataMapItems(parent, ((DataMap[]) dataMap)[i], index + 1));
                                    else
                                        ((TreeItem[]) dragSourceItem)[i] = null;
                                }
                                tree.setSelection(selectItems.toArray(new TreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        } else {
                            if (((ItemData) item.getData()).type == ItemData.TYPE_FOLDER | isContainer) {
                                for (int i = 0; i < ((DataMap[]) dataMap).length; i++) {
                                    if (!isContainer || ((ItemData) ((TreeItem[]) dragSourceItem)[i].getData()).type == ItemData.TYPE_FILE)
                                        selectItems.add(setDataMapItems(item, ((DataMap[]) dataMap)[i], -1));
                                    else
                                        ((TreeItem[]) dragSourceItem)[i] = null;
                                }
                                tree.setSelection(selectItems.toArray(new TreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        }

                    } else {
                        TreeItem[] items = tree.getItems();
                        int index = 0;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == item) {
                                index = i;
                                break;
                            }
                        }
                        if (pt.y < bounds.y + bounds.height / 3) {
                            for (int i = 0; i < ((DataMap[]) dataMap).length; i++) {
                                selectItems.add(setDataMapItems(tree, ((DataMap[]) dataMap)[i], index));
                            }
                            tree.setSelection(selectItems.toArray(new TreeItem[selectItems.size()]));
                            selectItems = null;
                        } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                            if (((ItemData) item.getData()).type == ItemData.TYPE_FOLDER | isContainer) {
                                for (int i = 0; i < ((DataMap[]) dataMap).length; i++) {
                                    if (!isContainer || ((ItemData) ((TreeItem[]) dragSourceItem)[i].getData()).type == ItemData.TYPE_FILE)
                                        selectItems.add(setDataMapItems(tree, ((DataMap[]) dataMap)[i], index + 1));
                                    else
                                        ((TreeItem[]) dragSourceItem)[i] = null;
                                }
                                tree.setSelection(selectItems.toArray(new TreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        } else {
                            if (((ItemData) item.getData()).type == ItemData.TYPE_FOLDER | isContainer) {
                                for (int i = 0; i < ((DataMap[]) dataMap).length; i++) {
                                    if (!isContainer || ((ItemData) ((TreeItem[]) dragSourceItem)[i].getData()).type == ItemData.TYPE_FILE)
                                        selectItems.add(setDataMapItems(item, ((DataMap[]) dataMap)[i], -1));
                                    else
                                        ((TreeItem[]) dragSourceItem)[i] = null;
                                }
                                tree.setSelection(selectItems.toArray(new TreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        }
                    }

                }
                tree.notifyListeners(SWT.Collapse, new Event());
            }
        });
    }
    /**
     * Diese Methode gehoert zum Drag&Drop System und erstellt ein neues
     * TreeItem
     * 
     * @param parent
     * @param text
     * @param itemData
     * @param index
     * @return
     */
    private TreeItem createTreeitem(Tree parent, String[] text, ItemData itemData, int index) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE, index);
        addItemData(treeItem, text, itemData);
        return treeItem;
    }
    /**
     * Diese Methode gehoert zum Drag&Drop System und erstellt ein neues
     * TreeItem
     * 
     * @param parent
     * @param text
     * @param itemData
     * @param index
     * @return
     */
    private TreeItem createTreeitem(TreeItem parent, String[] text, ItemData itemData, int index) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE, index);
        addItemData(treeItem, text, itemData);
        return treeItem;
    }
    /**
     * Diese Methode gehoert zum Drag&Drop System und erstellt ein neues
     * TreeItem
     * 
     * @param parent
     * @param text
     * @param itemData
     * @param index
     * @return
     */
    private TreeItem createTreeitem(Tree parent, String[] text, ItemData itemData) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE);
        addItemData(treeItem, text, itemData);
        return treeItem;
    }
    /**
     * Diese Methode gehoert zum Drag&Drop System und erstellt ein neues
     * TreeItem
     * 
     * @param parent
     * @param text
     * @param itemData
     * @param index
     * @return
     */
    private TreeItem createTreeitem(TreeItem parent, String[] text, ItemData itemData) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE);
        addItemData(treeItem, text, itemData);
        return treeItem;
    }
    /**
     * Mit dieser Methode werden die Daten dem TreeItem hinzugefuegt sie gehoert
     * zum Drag&Drop System
     * 
     * @param item
     * @param text
     * @param link
     * @param status
     * @param type
     */
    private void addItemData(final TreeItem item, String[] text, ItemData itemdata) {
        item.setData(itemdata);
        item.setText(text);
        item.setImage(JDSWTUtilities.getTreeImage(item.getDisplay(), itemdata.type, text[0], false));
    }
    /**
     * TODO ColumnReorder wurde rausgenommen weil die prioritäten jetzt ueber
     * Drag&Drop verwaltet werden
     * 
     * public boolean columnReorder(Tree tree, int pos, boolean order) {
     * TreeItem[] items = tree.getItems(); int columns = tree.getColumnCount();
     * Collator collator = Collator.getInstance(Locale.getDefault()); if (order) {
     * for (int i = 1; i < items.length; i++) { String value1 =
     * items[i].getText(pos); for (int j = 0; j < i; j++) { String value2 =
     * items[j].getText(pos);
     * 
     * if (collator.compare(value1, value2) > 0) { String[] values = new
     * String[columns]; for (int k = 0; k < columns; k++) values[k] =
     * items[i].getText(k); String[][] values2 = null; if
     * (items[i].getItemCount() > 0) { TreeItem[] subItems =
     * items[i].getItems(); values2 = new String[subItems.length][columns]; for
     * (int k = 0; k < subItems.length; k++) { for (int index = 0; index <
     * columns; index++) { values2[k][index] = subItems[k].getText(index); } } }
     * items[i].dispose(); TreeItem item = new TreeItem(tree, SWT.NONE, j);
     * item.setText(values); if (values2 != null) {
     * 
     * for (int k = 0; k < values2.length; k++) { TreeItem items2 = new
     * TreeItem(item, SWT.NONE); items2.setText(values2[k]); } }
     * 
     * items = tree.getItems(); break; } } } return false; } else { for (int i =
     * 1; i < items.length; i++) { String value1 = items[i].getText(pos); for
     * (int j = 0; j < i; j++) { String value2 = items[j].getText(pos);
     * 
     * if (collator.compare(value1, value2) < 0) { String[] values = new
     * String[columns]; for (int k = 0; k < columns; k++) values[k] =
     * items[i].getText(k); String[][] values2 = null; if
     * (items[i].getItemCount() > 0) { TreeItem[] subItems =
     * items[i].getItems(); values2 = new String[subItems.length][columns]; for
     * (int k = 0; k < subItems.length; k++) { for (int index = 0; index <
     * columns; index++) { values2[k][index] = subItems[k].getText(index); } } }
     * items[i].dispose(); TreeItem item = new TreeItem(tree, SWT.NONE, j);
     * item.setText(values); if (values2 != null) {
     * 
     * for (int k = 0; k < values2.length; k++) { TreeItem items2 = new
     * TreeItem(item, SWT.NONE); items2.setText(values2[k]); } } items =
     * tree.getItems(); break; } } } return true; } }
     */

    /**
     * Diese Methode Gehoert zum Drag&Drop System sie liest die Informationen
     * aus den Item und seinem Subitems
     */
    private DataMap getItemData(TreeItem item, int treeColumnCount) {
        String[] data = new String[treeColumnCount];
        for (int b = 0; b < data.length; b++) {
            data[b] = item.getText(b);
        }
        DataMap dat = new DataMap(data, (ItemData) item.getData());
        TreeItem[] itm = item.getItems();
        for (int i = 0; i < itm.length; i++) {
            if (itm[i].getItemCount() > 0)
                dat.dataMap.add(getItemData(itm[i], treeColumnCount));
            else {
                String[] datac = new String[treeColumnCount];
                for (int b = 0; b < data.length; b++) {
                    datac[b] = itm[i].getText(b);
                }
                DataMap dat2 = new DataMap(datac, (ItemData) itm[i].getData());
                dat.dataMap.add(dat2);
            }

        }
        return dat;
    }
    /**
     * Diese Methode Gehoert zum Drag&Drop System es werden die ausgelesenen
     * Items neu erstellt
     * 
     * @param parent
     * @param map
     * @param index
     * @return
     */
    private TreeItem setDataMapItems(TreeItem parent, DataMap map, int index) {

        TreeItem item;
        if (index > -1)
            item = createTreeitem(parent, map.text, map.itemData, index);
        else
            item = createTreeitem(parent, map.text, map.itemData);
        for (int j = 0; j < map.dataMap.size(); j++) {
            DataMap map2 = map.dataMap.get(j);
            TreeItem items = createTreeitem(item, map2.text, map2.itemData);
            for (int i = 0; i < map2.dataMap.size(); i++) {
                setDataMapItems(items, map2.dataMap.get(i), -1);
            }

        }
        return item;
    }

    /**
     * Diese Methode Gehoert zum Drag&Drop System es werden die ausgelesenen
     * Items neu erstellt
     * 
     * @param parent
     * @param map
     * @param index
     * @return
     */
    private TreeItem setDataMapItems(Tree parent, DataMap map, int index) {

        TreeItem item;
        if (index > -1)
            item = createTreeitem(parent, map.text, map.itemData, index);
        else
            item = createTreeitem(parent, map.text, map.itemData);
        for (int j = 0; j < map.dataMap.size(); j++) {
            DataMap map2 = map.dataMap.get(j);
            TreeItem items = createTreeitem(item, map2.text, map2.itemData);
            for (int i = 0; i < map2.dataMap.size(); i++) {
                setDataMapItems(items, map2.dataMap.get(i), -1);
            }

        }
        return item;
    }

    /**
     * gehoert auch zum Drag&Drop System und wird zum zwischenspeichern der
     * TreeItems Verwendet
     * 
     * @author DwD
     * 
     */

}
class DataMap {

    public Vector<DataMap> dataMap = new Vector<DataMap>();
    public String[] text;
    public ItemData itemData;

    public DataMap(String[] text, ItemData itemData) {
        this.text = text;
        this.itemData = itemData;
    }

}
