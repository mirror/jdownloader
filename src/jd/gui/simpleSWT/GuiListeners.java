package jd.gui.simpleSWT;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import jd.plugins.DownloadLink;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;

public class GuiListeners {
    // public static final String ST_DISK_READ_QUEUE_LENGTH =
    // "disk.read.queue.length";

    private HashMap<String, Listener> listeners = new HashMap<String, Listener>();
    private HashMap<String, Menu> listenerMenus = new HashMap<String, Menu>();
    private ControlListener folderControlListener;
    private GUIConfig guiConfig;
    private Shell mainGuiShell;
    private boolean btStartStopIsClicked = false;
    private MainGui mainGui;

    public GuiListeners(MainGui mainGui) {
        guiConfig = mainGui.guiConfig;
        mainGuiShell = mainGui.getShell();
        this.mainGui = mainGui;
    }
    public Listener getListener(String name) {
        return listeners.get(name);
    }
    public Listener addListener(String name, Listener listener) {
        return listeners.put(name, listener);
    }
    // TODO fuellen
    private void saveFile(String filename) {

    }
    private void initOpenfileListener() {
        listeners.put("openFile", new Listener() {
            public void handleEvent(Event event) {

                String[] out = getMultiFileDialog(mainGuiShell, guiConfig.btOpenFile);

                if (out != null) {
                    guiConfig.btOpenFile = out[0];
                    /**
                     * TODO FileOpen Programmieren
                     */
                    for (int i = 0; i < out.length; i++) {
                        System.out.println(out[i]);
                    }
                }
            }
        });
    }
    public Listener initBtOpenListener() {
        initOpenfileListener();
        final Menu menu = new Menu(mainGuiShell);
        listenerMenus.put("btOpen", menu);
        Listener btOpenListener = new Listener() {
            public void handleEvent(Event event) {
                if (event.detail == SWT.ARROW) {
                    ToolItem item = (ToolItem) event.widget;
                    Rectangle rect = item.getBounds();
                    Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
                    menu.setLocation(pt.x, pt.y + rect.height);
                    menu.setVisible(true);
                } else {
                    listeners.get("openFile").handleEvent(event);
                }
            }
        };
        listeners.put("btOpen", btOpenListener);
        return btOpenListener;
    }
    public Listener initSaveAsListener() {
        Listener listener = new Listener() {

            public void handleEvent(Event arg0) {
                String out = getFileDialog(mainGuiShell, guiConfig.save, 3);

                if (out != null) {
                    guiConfig.save = out;
                    saveFile(out);
                }

            }

        };
        listeners.put("saveAs", listener);
        return listener;
    }
    public Listener initSaveListener() {
        Listener listener = new Listener() {

            public void handleEvent(Event e) {
                if (guiConfig.save != null) {
                    saveFile(guiConfig.save);
                } else {
                    listeners.get("saveAs").handleEvent(e);
                }
            }

        };
        listeners.put("save", listener);
        return listener;
    }
    public MenuItem addListenerMenu(String listener, String name) {
        MenuItem menuItem = new MenuItem(listenerMenus.get(listener), SWT.NONE);
        menuItem.setText(name);
        return menuItem;
    }
    private String[] getMultiFileDialog(Shell shell, String file) {
        FileDialog dialog = new FileDialog(shell, SWT.MULTI);
        if (file != null) {
            dialog.setFileName(file);
        }
        if (dialog.open() == null)
            return null;
        String[] files = dialog.getFileNames();
        for (int i = 0; i < files.length; i++) {
        }
        String path = dialog.getFilterPath();
        if (path.substring(path.length() - 1) != System.getProperty("file.separator")) {
            path += System.getProperty("file.separator");
        }
        String[] names = dialog.getFileNames();
        for (int i = 0; i < names.length; i++) {
            names[i] = path + names[i];
        }

        return names;

    }
    public String getFileDialog(Shell shell, String file, int type) {
        if (type == 1) {
            DirectoryDialog dialog = new DirectoryDialog(shell);
            if (file != null) {
                dialog.setFilterPath(file);
            }
            return dialog.open();
        } else if (type == 2) {
            FileDialog dialog = new FileDialog(shell);
            if (file != null) {
                dialog.setFileName(file);
            }
            return dialog.open();
        } else {
            FileDialog dialog = new FileDialog(shell, SWT.SAVE);
            if (file != null) {
                dialog.setFileName(file);
            }
            return dialog.open();

        }

    }
    private void allMainGuiKeyListeners(Event e) {
        if ((e.keyCode == 'o') && (e.stateMask == SWT.CTRL)) {
            listeners.get("openFile").handleEvent(new Event());
        }
        if ((e.keyCode == 's') && (e.stateMask == SWT.CTRL)) {
            listeners.get("save").handleEvent(new Event());
        }
        if ((e.keyCode == 's') && (e.stateMask == SWT.SHIFT + SWT.CTRL) && mainGui.folder.getSelectionIndex() == 0) {
            listeners.get("saveAs").handleEvent(new Event());
        }
        if ((e.keyCode == 'n') && (e.stateMask == SWT.CTRL) && mainGui.folder.getSelectionIndex() == 0) {
            listeners.get("DownloadTab.newFolder").handleEvent(new Event());
        }
        if ((e.keyCode == 'n') && (e.stateMask == SWT.SHIFT + SWT.CTRL) && mainGui.folder.getSelectionIndex() == 0) {
            listeners.get("DownloadTab.newContainer").handleEvent(new Event());
        }
        if ((e.keyCode == '9') && (e.stateMask == SWT.CTRL)) {
            listeners.get("btStartStop").handleEvent(new Event());
        }
        if (e.keyCode == SWT.F11) {
            listeners.get("btPreferences").handleEvent(new Event());
        }

    }
    
    public Listener initMainGuiKeyListener()
    {
        Listener mainGuiKeyListener = new Listener() {
            public void handleEvent(Event e) {
                allMainGuiKeyListeners(e);
                
            }
        };
        listeners.put("mainGuiKey", mainGuiKeyListener);
        return mainGuiKeyListener;
    }

    public Listener initTrDownloadKeyListener() {
        Listener trDownloadKeyListener = new Listener() {

            public void handleEvent(Event e) {
                allMainGuiKeyListeners(e);
                if ((e.keyCode == SWT.DEL) && (((Tree) e.widget).getSelectionCount() > 0)) {
                    listeners.get("DownloadTab.delete").handleEvent(new Event());
                }
                if ((e.keyCode == 'c') && (e.stateMask == SWT.CTRL)) {
                    copy((ExtendedTree) ((Tree) e.widget).getData());
                }
                if ((e.keyCode == SWT.F2) && (((ExtendedTree) ((Tree) e.widget).getData()).getOwnSelection().length > 0)) {
                    getListener("DownloadTab.rename").handleEvent(new Event());
                }
                
            }

        };
        listeners.put("trDownloadKey", trDownloadKeyListener);
        return trDownloadKeyListener;
    }
    public Listener initToolBarBtSetEnabledListener() {
        Listener toolBarBtSetEnabledListener = new Listener() {
            public void handleEvent(Event e) {
                mainGui.toolBarBtSetEnabled();
            }
        };
        listeners.put("toolBarBtSetEnabled", toolBarBtSetEnabledListener);
        return toolBarBtSetEnabledListener;
    }
    public ControlListener setFolderControlListener(final Table tablePluginActivities) {

        folderControlListener = new ControlAdapter() {
            public void controlResized(ControlEvent e) {
                final CTabFolder folder = mainGui.folder;
                Rectangle area = folder.getClientArea();
                Point preferredSize = tablePluginActivities.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                int width = area.width - 2 * tablePluginActivities.getBorderWidth();
                if (preferredSize.y > area.height + tablePluginActivities.getHeaderHeight()) {
                    Point vBarSize = tablePluginActivities.getVerticalBar().getSize();
                    width -= vBarSize.x;
                }
                width /= 3;
                Point oldSize = tablePluginActivities.getSize();
                if (oldSize.x > area.width) {
                    TableColumn[] columns = tablePluginActivities.getColumns();
                    for (int i = 0; i < columns.length; i++) {
                        columns[i].setWidth(width);
                    }
                    tablePluginActivities.setSize(area.width, area.height);
                } else {
                    tablePluginActivities.setSize(area.width, area.height);
                    TableColumn[] columns = tablePluginActivities.getColumns();
                    for (int i = 0; i < columns.length; i++) {
                        columns[i].setWidth(width);
                    }
                }
            }
        };
        return folderControlListener;
    }
    public ControlListener getFolderControlListener() {
        return folderControlListener;
    }
    public Listener initBtPreferencesListener() {
        Listener btPreferencesListener = new Listener() {
            PreferencesTab preferencesTab = null;
            public void handleEvent(Event e) {
                if (preferencesTab == null || preferencesTab.tbPreferences.isDisposed()) {
                    preferencesTab = new PreferencesTab(mainGui);
                }

                listeners.get("toolBarBtSetEnabled").handleEvent(e);
            }
        };
        listeners.put("btPreferences", btPreferencesListener);
        return btPreferencesListener;
    }
    public Listener initBtStartStopListener(final ToolItem btStartPause) {
        final Image btStartImage = btStartPause.getImage();
        final Image btStopImage = JDSWTUtilities.getImageSwt("stop");
        Listener btStartStopListener = new Listener() {
            public void handleEvent(Event e) {
                if (btStartStopIsClicked) {
                    btStartStopIsClicked = false;
                    btStartPause.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btStart.desc"));
                    btStartPause.setImage(btStartImage);
                } else {
                    btStartStopIsClicked = true;
                    btStartPause.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btStop.desc"));
                    btStartPause.setImage(btStopImage);
                }
            }
        };
        listeners.put("btStartStop", btStartStopListener);
        return btStartStopListener;
    }
    public Listener initBtInfoListener() {
        final Menu btInfoMenu = new Menu(mainGuiShell);
        listenerMenus.put("btInfo", btInfoMenu);
        Listener btInfoListener = new Listener() {
            public void handleEvent(Event event) {

                ToolItem item = (ToolItem) event.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
                btInfoMenu.setLocation(pt.x, pt.y + rect.height);
                btInfoMenu.setVisible(true);

            }
        };
        listeners.put("btInfo", btInfoListener);
        return btInfoListener;
    }
    public Listener initTrCompKeyListener() {
        Listener trCompKeyListener = new Listener() {

            public void handleEvent(Event e) {
                allMainGuiKeyListeners(e);
                if ((e.keyCode == SWT.DEL) && (((Tree) e.widget).getSelectionCount() > 0)) {
                    listeners.get("CompletedTab.delete").handleEvent(new Event());
                }
                if ((e.keyCode == 'c') && (e.stateMask == SWT.CTRL)) {
                    copy((ExtendedTree) ((Tree) e.widget).getData());
                }
                
            }
        };
        listeners.put("trCompletedKey", trCompKeyListener);
        return trCompKeyListener;
    }
    private void copy(ExtendedTree trCompleted) {
        ExtendedTreeItem[] items = (ExtendedTreeItem[]) trCompleted.getSelection();
        String evd = "";
        if (items.length > 0) {
            for (int i = 0; i < items.length - 1; i++) {
                DownloadLink downloadLink =  items[i].getDownloadLink();
                evd += ((downloadLink != null) ? downloadLink.getEncryptedUrlDownload() : items[i].getText());
                evd += System.getProperty("line.separator");
            }
            DownloadLink downloadLink =  items[items.length - 1].getDownloadLink();
            evd += ((downloadLink != null) ? downloadLink.getUrlDownload() : items[items.length - 1].getText());
            Clipboard clipboard = new Clipboard(trCompleted.tree.getDisplay());
            clipboard.setContents(new String[]{evd}, new Transfer[]{TextTransfer.getInstance()});
        }
    }
    public Listener addTreeCopyListener(final ExtendedTree trCompleted, String name) {
        Listener copyl = new Listener() {
            public void handleEvent(Event event) {
                copy(trCompleted);
            }
        };
        listeners.put(name, copyl);
        return copyl;
    }
    public Listener initBtInfoAboutListener() {
        Listener btInfoAboutListener = new Listener() {
            public void handleEvent(Event event) {
                new AboutDialog(mainGuiShell).open();
            }
        };
        listeners.put("btInfoAbout", btInfoAboutListener);
        return btInfoAboutListener;
    }

    public Listener initMainGuiCloseListener(final Shell shell) {
        Listener mainGuiCloseListener = new Listener() {
            public void handleEvent(Event event) {
                MessageBox mbMainWindowClose = new MessageBox(shell, SWT.APPLICATION_MODAL | SWT.YES | SWT.NO);
                mbMainWindowClose.setText("Information");
                mbMainWindowClose.setMessage("Close the shell?");
                boolean isYes = mbMainWindowClose.open() == SWT.YES;
                event.doit = isYes;
                if (!isYes)
                    return;

                try {
                    // guiConfig.setQColumnOrder(trDownload.getColumnOrder());
                    // //TODO Bug der durch TreeEditor beim verschieben der
                    // Columns entsteht beheben
                    guiConfig.DownloadColumnOrder = mainGui.downloadTab.trDownload.tree.getColumnOrder();
                    guiConfig.isMaximized = shell.getMaximized();
                    ObjectOutputStream objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(mainGui.guiConfigFile)));
                    objOut.writeObject(guiConfig);
                    objOut.close();

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        };
        listeners.put("mainGuiClose", mainGuiCloseListener);
        return mainGuiCloseListener;
    }
}
