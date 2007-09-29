package jd.gui.simpleSWT;

/**
 * Der HauptSWTGUI
 * @author DwD
 *
 */

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.Vector;

import jd.event.ControlEvent;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.plugins.event.PluginEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class MainGui extends org.eclipse.swt.widgets.Composite implements UIInterface {

    private Composite compStatusBar;
    public CTabFolder folder;
    public GUIConfig guiConfig = new GUIConfig();
    public File guiConfigFile = new File("guiconf.ser");
    private boolean isDownloadTree = true;
    public GuiListeners guiListeners;
    private CompletedTab completedTab;
    public DownloadTab downloadTab;
    // private boolean[] queuedColClicked; TODO von unten

    private ToolBar toolBar;
    private ToolItem btPreferences;
    private ToolItem btInfo;
    private ToolItem btGoLastDown;
    private ToolItem btGoDown;
    private ToolItem btGoUp;
    private ToolItem btGoLastUp;
    private ToolItem btDelete;
    private ToolItem btCopy;
    private ToolItem btPaste;
    private ToolItem btStartStop;
    private ToolItem btOpen;
    private Vector<UIListener> uiListener;

    /**
     * Es werden die Bilder fuer den GUI in den Speicher geladen
     * 
     */

    private void loadImages(Display display) {
        ClassLoader cl = getClass().getClassLoader();
        JDSWTUtilities.addImageSwt("download", cl.getResourceAsStream("img/swt/download24.png"), display);
        JDSWTUtilities.addImageSwt("log", cl.getResourceAsStream("img/swt/log24.png"), display);
        JDSWTUtilities.addImageSwt("plugins", cl.getResourceAsStream("img/swt/plugins24.png"), display);
        JDSWTUtilities.addImageSwt("statistic", cl.getResourceAsStream("img/swt/statistic24.png"), display);
        JDSWTUtilities.addImageSwt("completed", cl.getResourceAsStream("img/swt/completed24.png"), display);
        JDSWTUtilities.addImageSwt("open", cl.getResourceAsStream("img/swt/open.png"), display);
        JDSWTUtilities.addImageSwt("copy", cl.getResourceAsStream("img/swt/copy.png"), display);
        JDSWTUtilities.addImageSwt("delete", cl.getResourceAsStream("img/swt/delete.png"), display);
        JDSWTUtilities.addImageSwt("go-down", cl.getResourceAsStream("img/swt/go-down.png"), display);
        JDSWTUtilities.addImageSwt("go-lastdown", cl.getResourceAsStream("img/swt/go-lastdown.png"), display);
        JDSWTUtilities.addImageSwt("go-up", cl.getResourceAsStream("img/swt/go-up.png"), display);
        JDSWTUtilities.addImageSwt("go-lastup", cl.getResourceAsStream("img/swt/go-lastup.png"), display);
        JDSWTUtilities.addImageSwt("info", cl.getResourceAsStream("img/swt/info.png"), display);
        JDSWTUtilities.addImageSwt("paste", cl.getResourceAsStream("img/swt/paste.png"), display);
        JDSWTUtilities.addImageSwt("stop", cl.getResourceAsStream("img/swt/stop.png"), display);
        JDSWTUtilities.addImageSwt("preferences", cl.getResourceAsStream("img/swt/preferences.png"), display);
        JDSWTUtilities.addImageSwt("start", cl.getResourceAsStream("img/swt/start.png"), display);
        JDSWTUtilities.addImageSwt("container", cl.getResourceAsStream("img/swt/mime/container.png"), display);
        JDSWTUtilities.addImageSwt("default", cl.getResourceAsStream("img/swt/mime/default.png"), display);
        JDSWTUtilities.addImageSwt("folder", cl.getResourceAsStream("img/swt/mime/folder.png"), display);
    }
    public MainGui() {
        super(new Shell(Display.getDefault()), SWT.NULL);
        initGUI();
    }

    private void initGUI() {
        uiListener = new Vector<UIListener>();
        if ((guiConfigFile).isFile()) {
            ObjectInputStream objIn;
            try {
                objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream("guiconf.ser")));
                guiConfig = (GUIConfig) objIn.readObject();
                objIn.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        final Shell shell = this.getShell();
        final Display display = this.getDisplay();
        guiListeners = new GuiListeners(this);
        this.setSize(guiConfig.GUIsize[0], guiConfig.GUIsize[1]);

        loadImages(display);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout thisLayout = new GridLayout();
        thisLayout.marginHeight = 0;
        thisLayout.horizontalSpacing = 0;
        thisLayout.marginWidth = 3;
        thisLayout.verticalSpacing = 0;
        this.setLayout(thisLayout);
        {
            toolBar = new ToolBar(this, (SWT.getPlatform().equals("gtk") ? SWT.NONE : SWT.FLAT));
            toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            {
                btOpen = JDSWTUtilities.toolBarItem(display, toolBar, "open", SWT.DROP_DOWN);
                btOpen.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btOpen.desc"));
                btOpen.addListener(SWT.Selection, guiListeners.initBtOpenListener());
                MenuItem btOpenOpen = guiListeners.addListenerMenu("btOpen", JDSWTUtilities.getSWTResourceString("MainGui.btOpenOpen.name"));
                btOpenOpen.addListener(SWT.Selection, guiListeners.getListener("openFile"));
                MenuItem btOpenSave = guiListeners.addListenerMenu("btOpen", JDSWTUtilities.getSWTResourceString("MainGui.btOpenSave.name"));
                btOpenSave.addListener(SWT.Selection, guiListeners.initSaveListener());
                MenuItem btOpenSaveAs = guiListeners.addListenerMenu("btOpen", JDSWTUtilities.getSWTResourceString("MainGui.btOpenSaveAs.name"));
                btOpenSaveAs.addListener(SWT.Selection, guiListeners.initSaveAsListener());
                new ToolItem(toolBar, SWT.SEPARATOR);
                btStartStop = JDSWTUtilities.toolBarItem(display, toolBar, "start");
                btStartStop.addListener(SWT.Selection, guiListeners.initBtStartStopListener(btStartStop));
                btStartStop.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btStart.desc"));
                new ToolItem(toolBar, SWT.SEPARATOR);
                btPaste = JDSWTUtilities.toolBarItem(display, toolBar, "paste");
                btPaste.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btPaste.desc"));
                btCopy = JDSWTUtilities.toolBarItem(display, toolBar, "copy");
                btCopy.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btCopy.desc"));
                btDelete = JDSWTUtilities.toolBarItem(display, toolBar, "delete");
                btDelete.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btDelete.desc"));
                new ToolItem(toolBar, SWT.SEPARATOR);
                btGoLastUp = JDSWTUtilities.toolBarItem(display, toolBar, "go-lastup");
                btGoLastUp.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoLastUp.desc"));
                btGoUp = JDSWTUtilities.toolBarItem(display, toolBar, "go-up");
                btGoUp.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoUp.desc"));
                btGoDown = JDSWTUtilities.toolBarItem(display, toolBar, "go-down");
                btGoDown.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoDown.desc"));
                btGoLastDown = JDSWTUtilities.toolBarItem(display, toolBar, "go-lastdown");
                btGoLastDown.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoLastDown.desc"));
                new ToolItem(toolBar, SWT.SEPARATOR);
                btPreferences = JDSWTUtilities.toolBarItem(display, toolBar, "preferences");
                btPreferences.addListener(SWT.Selection, guiListeners.initBtPreferencesListener());
                btPreferences.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btPreferences.desc"));
                btInfo = JDSWTUtilities.toolBarItem(display, toolBar, "info", SWT.DROP_DOWN);
                btInfo.addListener(SWT.Selection, guiListeners.initBtInfoListener());
                MenuItem btInfoHelp = guiListeners.addListenerMenu("btInfo", JDSWTUtilities.getSWTResourceString("MainGui.btInfoHelp.name"));
                MenuItem btInfoUpdate = guiListeners.addListenerMenu("btInfo", JDSWTUtilities.getSWTResourceString("MainGui.btInfoUpdate.name"));
                MenuItem btInfoAbout = guiListeners.addListenerMenu("btInfo", JDSWTUtilities.getSWTResourceString("MainGui.btInfoAbout.name"));
                btInfoAbout.addListener(SWT.Selection, guiListeners.initBtInfoAboutListener());
            }

        }
        {
            folder = new CTabFolder(this, SWT.BORDER);
            folder.addListener(SWT.KeyDown,guiListeners.initMainGuiKeyListener());
            folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            folder.setSimple(false);
            folder.setUnselectedImageVisible(true);
            folder.setUnselectedCloseVisible(true);
            folder.setBorderVisible(true);
            folder.setSelectionForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
            folder.setSelectionBackground(new Color[]{display.getSystemColor(SWT.COLOR_GRAY), display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_GRAY)}, new int[]{40, 60}, true);
            folder.setFocus();
            downloadTab = new DownloadTab(this);
            folder.setSelection(0);

            folder.addListener(SWT.Selection, guiListeners.getListener("toolBarBtSetEnabled"));
            btDelete.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.delete"));
            btCopy.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.copy"));
            btGoUp.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.goUp"));
            btGoDown.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.goDown"));
            btGoLastUp.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.goLastUp"));
            btGoLastDown.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.goLastDown"));

            completedTab = new CompletedTab(folder, guiListeners);
            new LoggerTab(this);

            new StatisticsTab(this);

            new PluginActivitiesTab(this);

        }
        {
            compStatusBar = new Composite(this, SWT.NONE);
            GridLayout compStatusBarLayout = new GridLayout();
            compStatusBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            compStatusBarLayout.makeColumnsEqualWidth = true;
            compStatusBar.setLayout(compStatusBarLayout);
        }
        this.addListener(SWT.Resize, new Listener() {

            public void handleEvent(Event e) {
                if (!shell.getMaximized()) {

                    Point size = shell.getSize();
                    guiConfig.GUIsize = new int[]{size.x, size.y};

                    for (int i = 0; i < guiConfig.DownloadColumnWidht.length; i++) {
                        downloadTab.trDownload.tree.getColumn(i).setWidth(guiConfig.DownloadColumnWidht[i]);
                    }

                } else {

                    if (guiConfig.DownloadColumnWidhtMaximized != null)
                        for (int i = 0; i < guiConfig.DownloadColumnWidhtMaximized.length; i++) {
                            downloadTab.trDownload.tree.getColumn(i).setWidth(guiConfig.DownloadColumnWidhtMaximized[i]);
                        }

                }

            }
        });
        this.layout();
        guiListeners.getListener("toolBarBtSetEnabled").handleEvent(new Event());
        Point size = this.getSize();
        shell.setText(JDSWTUtilities.getSWTResourceString("MainGui.name"));
        shell.setLayout(new FillLayout());
        Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
        shell.setSize(shellBounds.width, shellBounds.height);
        shell.addListener(SWT.Close, guiListeners.initMainGuiCloseListener(shell));
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        shell.setLocation((d.width - guiConfig.GUIsize[0]) / 2, (d.height - guiConfig.GUIsize[1]) / 2);
        shell.open();

        if (guiConfig.isMaximized) {
            shell.setLocation(shell.getLocation());
            shell.setMaximized(true);
        }
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    public void toolBarBtSetEnabled() {

        if ((folder.getSelectionIndex() == 0) && downloadTab.trDownload.getSelectionCount() > 0) {
            ExtendedTreeItem[] trDownloadSelection = downloadTab.trDownload.getOwnSelection();
            if (!isDownloadTree) {
                isDownloadTree = true;
                btDelete.removeListener(SWT.Selection, guiListeners.getListener("CompletedTab.delete"));
                btDelete.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.delete"));
                btCopy.removeListener(SWT.Selection, guiListeners.getListener("CompletedTab.copy"));
                btCopy.addListener(SWT.Selection, guiListeners.getListener("DownloadTab.copy"));
            }
            btDelete.setEnabled(true);
            btCopy.setEnabled(true);
            if (trDownloadSelection.length < 1) {
                btGoLastUp.setEnabled(false);
                btGoUp.setEnabled(false);
                btGoDown.setEnabled(false);
                btGoLastDown.setEnabled(false);
            } else {
                if (downloadTab.trDownload.tree.getItem(0) == (trDownloadSelection[0].item)) {
                    btGoLastUp.setEnabled(false);
                    btGoUp.setEnabled(false);
                } else {
                    btGoLastUp.setEnabled(true);
                    btGoUp.setEnabled(true);
                }
                if (downloadTab.trDownload.tree.getItem(downloadTab.trDownload.tree.getItemCount() - 1) == (trDownloadSelection[trDownloadSelection.length - 1].item)) {
                    btGoDown.setEnabled(false);
                    btGoLastDown.setEnabled(false);
                } else {
                    btGoDown.setEnabled(true);
                    btGoLastDown.setEnabled(true);
                }
            }
        } else if ((folder.getSelectionIndex() == 1) && completedTab.trCompleted.getSelectionCount() > 0) {
            if (isDownloadTree) {
                isDownloadTree = false;
                btDelete.removeListener(SWT.Selection, guiListeners.getListener("DownloadTab.delete"));
                btDelete.addListener(SWT.Selection, guiListeners.getListener("CompletedTab.delete"));
                btCopy.removeListener(SWT.Selection, guiListeners.getListener("DownloadTab.copy"));
                btCopy.addListener(SWT.Selection, guiListeners.getListener("CompletedTab.copy"));
            }
            btDelete.setEnabled(true);
            btCopy.setEnabled(true);
            btGoLastUp.setEnabled(false);
            btGoUp.setEnabled(false);
            btGoDown.setEnabled(false);
            btGoLastDown.setEnabled(false);
        } else {
            btDelete.setEnabled(false);
            btCopy.setEnabled(false);
            btGoLastUp.setEnabled(false);
            btGoUp.setEnabled(false);
            btGoDown.setEnabled(false);
            btGoLastDown.setEnabled(false);
        }
    }
    /**
     * Fügt die links zum Linkgrabber hinzu.
     */
    public void addLinksToGrabber(Vector<DownloadLink> links) {
        // TODO Auto-generated method stub

    }

    public void addUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.add(listener);
        }
    }

    public void removeUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.remove(listener);
        }
    }
    /**
     * Eine Liste der möglichen UIEvents ids findet man unter jd.event.UIEvent.
     * Damit kann die ui events an den downloadcontroller schicken
     */
    public void fireUIEvent(UIEvent uiEvent) {
        synchronized (uiListener) {
            Iterator<UIListener> recIt = uiListener.iterator();

            while (recIt.hasNext()) {
                ((UIListener) recIt.next()).uiEvent(uiEvent);
            }
        }
    }
    /**
     * Führt controllevents aus.
     * 
     * in der klasse ControlEvent sind alle möglichen eventIDs zu sehen * oft
     * werden dem event parameter übergeben die mit event.getParameter()
     * abgefragt werden die parameter sind meistens mit der ventid dokumentiert
     */
    public void deligatedControlEvent(ControlEvent event) {

        switch (event.getID()) {
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE :

                break;
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE :

                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE :

                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE :

                break;
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED :

                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE :

                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE :
                break;
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED :

            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED :

                break;
        }

    }
    /**
     * auch hier sind in PluginEvent die Event ids dokumentiert. oft werden dem
     * event parameter übergeben die mit event.getParameter() abgefragt werden
     * die parameter sind meistens mit der ventid dokumentiert
     */
    public void deligatedPluginEvent(PluginEvent event) {

        if (event.getSource() instanceof PluginForHost && event.getEventID() == PluginEvent.PLUGIN_DATA_CHANGED) {

            return;
        }
        if (event.getSource() instanceof PluginForDecrypt && event.getEventID() == PluginEvent.PLUGIN_DATA_CHANGED) {

            return;
        }

        if (event.getSource() instanceof PluginForSearch && event.getEventID() == PluginEvent.PLUGIN_DATA_CHANGED) {

            return;
        }
        if (event.getSource() instanceof PluginForHost) {

            return;
        }
        if (event.getSource() instanceof PluginForDecrypt || event.getSource() instanceof PluginForSearch) {

            return;
        }

    }

    /**
     * Muss einen string mit dem captchacode zurückgeben. captchaAddress ist der
     * pfad zur lokalen captchafile, Plugin das entsprechende PLugin
     */
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress) {

        return null;
    }

    /**
     * Gibt alle downloadlinks in der downloadtabelle zurück
     */
    public Vector<DownloadLink> getDownloadLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Setzt die downloadLinks in die tabelle und überschreibt die vorhandenen
     */
    public void setDownloadLinks(Vector<DownloadLink> downloadLinks) {
        // TODO Auto-generated method stub

    }

    /**
     * Zeigt einen Confirm dialog mit string an
     */
    public boolean showConfirmDialog(String string) {
        // TODO Auto-generated method stub
        return true;

    }
    /**
     * zeugt einen messagedialog an
     */
    public void showMessageDialog(String string) {
        // TODO Auto-generated method stub

    }

}
