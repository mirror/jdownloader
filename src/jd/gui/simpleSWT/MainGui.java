package jd.gui.simpleSWT;

/**
 * Der HauptSWTGUI
 * @author DwD
 *
 */

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.SWT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

public class MainGui extends org.eclipse.swt.widgets.Composite {

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

    public MainGui(org.eclipse.swt.widgets.Composite parent, int style) {
        super(parent, style);
        initGUI();
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
    }

    private void initGUI() {
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
            folder.addKeyListener(guiListeners.initMainGuiKeyListener());
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
                        downloadTab.trDownload.getColumn(i).setWidth(guiConfig.DownloadColumnWidht[i]);
                    }

                } else {

                    if (guiConfig.DownloadColumnWidhtMaximized != null)
                        for (int i = 0; i < guiConfig.DownloadColumnWidhtMaximized.length; i++) {
                            downloadTab.trDownload.getColumn(i).setWidth(guiConfig.DownloadColumnWidhtMaximized[i]);
                        }

                }

            }
        });
        this.layout();
        guiListeners.getListener("toolBarBtSetEnabled").handleEvent(new Event());
    }

    public void toolBarBtSetEnabled() {

        if ((folder.getSelectionIndex() == 0) && downloadTab.trDownload.getSelectionCount() > 0) {
            TreeItem[] trDownloadSelection = DownloadTab.getSelection(downloadTab.trDownload);
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
                if (downloadTab.trDownload.getItem(0) == (trDownloadSelection[0])) {
                    btGoLastUp.setEnabled(false);
                    btGoUp.setEnabled(false);
                } else {
                    btGoLastUp.setEnabled(true);
                    btGoUp.setEnabled(true);
                }
                if (downloadTab.trDownload.getItem(downloadTab.trDownload.getItemCount() - 1) == (trDownloadSelection[trDownloadSelection.length - 1])) {
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

}
