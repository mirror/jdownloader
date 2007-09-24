package jd.gui.simpleSWT;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.swt.widgets.TreeItem;

public class GuiListeners {
    // public static final String ST_DISK_READ_QUEUE_LENGTH =
    // "disk.read.queue.length";

    private static HashMap<String, Listener> listeners = new HashMap<String, Listener>();
    private static HashMap<String, KeyListener> keyListeners = new HashMap<String, KeyListener>();
    private static HashMap<String, Menu> listenerMenus = new HashMap<String, Menu>();
    private static ControlListener folderControlListener;
    private static GUIConfig guiConfig;
    private static Shell mainGuiShell;
    private static boolean btStartStopIsClicked = false;

    public static void setGuiConfig(GUIConfig guiConfig) {
        GuiListeners.guiConfig = guiConfig;
    }
    public static void setMainGuiShell(Shell mainGuiShell) {
        GuiListeners.mainGuiShell = mainGuiShell;
    }
    public static Listener getListener(String name) {
        return listeners.get(name);
    }
    public static KeyListener getKeyListener(String name) {
        return keyListeners.get(name);
    }
    public static Listener addListener(String name, Listener listener) {
        return listeners.put(name, listener);
    }
    public static KeyListener addKeyListener(String name, KeyListener keyListener) {
        return keyListeners.put(name, keyListener);
    }
    // TODO fuellen
    private static void saveFile(String filename) {

    }
    private static void initOpenfileListener() {
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
    public static Listener initBtOpenListener() {
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
    public static Listener initSaveAsListener() {
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
    public static Listener initSaveListener() {
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
    public static MenuItem addListenerMenu(String listener, String name) {
        MenuItem menuItem = new MenuItem(listenerMenus.get(listener), SWT.NONE);
        menuItem.setText(name);
        return menuItem;
    }
    private static String[] getMultiFileDialog(Shell shell, String file) {
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
    private static String getFileDialog(Shell shell, String file, int type) {
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
    private static void allMainGuiKeyListeners(KeyEvent e) {
        if ((e.keyCode == 'o') && (e.stateMask == SWT.CTRL)) {
            listeners.get("openFile").handleEvent(new Event());
        }
        if ((e.keyCode == 's') && (e.stateMask == SWT.CTRL)) {
            listeners.get("save").handleEvent(new Event());
        }
        if ((e.keyCode == 's') && (e.stateMask == SWT.SHIFT + SWT.CTRL) && MainGui.getFolder().getSelectionIndex() == 0) {
            listeners.get("saveAs").handleEvent(new Event());
        }
        if ((e.keyCode == 'n') && (e.stateMask == SWT.CTRL) && MainGui.getFolder().getSelectionIndex() == 0) {
            listeners.get("DownloadTab.newFolder").handleEvent(new Event());
        }
        if ((e.keyCode == 'n') && (e.stateMask == SWT.SHIFT + SWT.CTRL) && MainGui.getFolder().getSelectionIndex() == 0) {
            listeners.get("DownloadTab.newContainer").handleEvent(new Event());
        }
        if ((e.keyCode == '9') && (e.stateMask == SWT.CTRL)) {
            listeners.get("btStartStop").handleEvent(new Event());
        }
        if (e.keyCode == SWT.F11) {
            listeners.get("btPreferences").handleEvent(new Event());
        }

    }
    public static KeyListener initMainGuiKeyListener() {
        KeyListener mainGuiKeyListener = new KeyListener() {
            public void keyPressed(KeyEvent e) {
                allMainGuiKeyListeners(e);
            }
            public void keyReleased(KeyEvent e) {
            }
        };
        keyListeners.put("mainGui", mainGuiKeyListener);
        return mainGuiKeyListener;
    }
    public static KeyListener initTrDownloadKeyListener() {
        KeyListener trDownloadKeyListener = new KeyListener() {

            public void keyPressed(KeyEvent e) {
                allMainGuiKeyListeners(e);
                if ((e.keyCode == SWT.DEL) && (((Tree) e.widget).getSelectionCount() > 0)) {
                    listeners.get("DownloadTab.delete").handleEvent(new Event());
                }
                if ((e.keyCode == 'c') && (e.stateMask == SWT.CTRL)) {
                    copy((Tree) e.widget);
                }
                if ((e.keyCode == SWT.F2) && (DownloadTab.getSelection(DownloadTab.trDownload).length > 0)) {
                    GuiListeners.getListener("DownloadTab.rename").handleEvent(new Event());
                }
            }

            public void keyReleased(KeyEvent e) {

            }

        };
        keyListeners.put("trDownload", trDownloadKeyListener);
        return trDownloadKeyListener;
    }
    public static Listener initToolBarBtSetEnabledListener() {
        Listener toolBarBtSetEnabledListener = new Listener() {
            public void handleEvent(Event e) {
                MainGui.toolBarBtSetEnabled();
            }
        };
        listeners.put("toolBarBtSetEnabled", toolBarBtSetEnabledListener);
        return toolBarBtSetEnabledListener;
    }
    public static ControlListener setFolderControlListener(final Table tablePluginActivities) {

        folderControlListener = new ControlAdapter() {
            public void controlResized(ControlEvent e) {
                final CTabFolder folder = MainGui.getFolder();
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
    public static ControlListener getFolderControlListener() {
        return folderControlListener;
    }
    public static Listener initBtPreferencesListener() {
        Listener btPreferencesListener = new Listener() {
            public void handleEvent(Event e) {
                PreferencesTab.initpreferences();
                listeners.get("toolBarBtSetEnabled").handleEvent(e);
            }
        };
        listeners.put("btPreferences", btPreferencesListener);
        return btPreferencesListener;
    }
    public static Listener initBtStartStopListener(final ToolItem btStartPause) {
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
    public static Listener initBtInfoListener() {
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
    public static KeyListener initTrCompKeyListener() {
        KeyListener trCompKeyListener = new KeyListener() {

            public void keyPressed(KeyEvent e) {
                allMainGuiKeyListeners(e);
                if ((e.keyCode == SWT.DEL) && (((Tree) e.widget).getSelectionCount() > 0)) {
                    listeners.get("CompletedTab.delete").handleEvent(new Event());
                }
                if ((e.keyCode == 'c') && (e.stateMask == SWT.CTRL)) {
                    copy((Tree) e.widget);
                }
            }

            public void keyReleased(KeyEvent e) {

            }

        };
        keyListeners.put("trCompleted", trCompKeyListener);
        return trCompKeyListener;
    }
    private static void copy(Tree tree) {
        TreeItem[] items = tree.getSelection();
        String evd = "";
        if (items.length > 0) {
            for (int i = 0; i < items.length - 1; i++) {
                ItemData itd = (ItemData) items[i].getData();
                evd += ((itd.link != null) ? itd.link : items[i].getText());
                evd += System.getProperty("line.separator");
            }
            ItemData itd = (ItemData) items[items.length - 1].getData();
            evd += ((itd.link != null) ? itd.link : items[items.length - 1].getText());
            Clipboard clipboard = new Clipboard(tree.getDisplay());
            clipboard.setContents(new String[]{evd}, new Transfer[]{TextTransfer.getInstance()});
        }
    }
    public static Listener addTreeCopyListener(final Tree tree, String name) {
        Listener copyl = new Listener() {
            public void handleEvent(Event event) {
                copy(tree);
            }
        };
        listeners.put(name, copyl);
        return copyl;
    }
    public static Listener initBtInfoAboutListener() {
        Listener btInfoAboutListener = new Listener() {
            public void handleEvent(Event event) {
                new AboutDialog(mainGuiShell).open();
            }
        };
        listeners.put("btInfoAbout", btInfoAboutListener);
        return btInfoAboutListener;
    }

    public static Listener initMainGuiCloseListener(final Shell shell) {
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
                    guiConfig.DownloadColumnOrder = DownloadTab.trDownload.getColumnOrder();
                    guiConfig.isMaximized = shell.getMaximized();
                    ObjectOutputStream objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(MainGui.guiConfigFile)));
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
